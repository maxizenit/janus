package org.janus.statestore.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.statestore.mapper.DegradationStateUpdateMapper;
import org.janus.statestore.model.AdminDegradationState;
import org.janus.statestore.model.DegradationState;
import org.janus.statestore.model.DegradationStateUpdate;
import org.janus.statestore.model.DegradationStateUpdateSource;
import org.janus.statestore.model.EffectiveDegradationState;
import org.janus.statestore.model.SourceDegradationState;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class DegradationStateService {

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final DegradationStateUpdateMapper stateUpdateMapper;

  public List<DegradationState> getDegradationStates(List<String> degradationIds) {
    List<AdminDegradationState> adminStates = getAdminDegradationStates(degradationIds);

    List<DegradationState> result =
        adminStates.stream()
            .map(
                state ->
                    new DegradationState(state.degradationId(), state.effectiveState().value()))
            .toList();

    log.debug(
        "Effective degradation state lookup completed: requested={}, resolved={}, missing={}",
        degradationIds.size(),
        result.size(),
        degradationIds.size() - result.size());

    return result;
  }

  public List<AdminDegradationState> getAdminDegradationStates(List<String> degradationIds) {
    if (CollectionUtils.isEmpty(degradationIds)) {
      log.debug("Skipping admin degradation state lookup: empty degradationIds");
      return Collections.emptyList();
    }

    log.debug("Admin degradation state lookup started: degradationCount={}", degradationIds.size());

    Map<String, Map<DegradationStateUpdateSource, SourceDegradationState>> sourceStatesById =
        loadSourceStatesByDegradationId(degradationIds);

    List<AdminDegradationState> result = new ArrayList<>(degradationIds.size());
    int missingCount = 0;

    for (String degradationId : degradationIds) {
      Map<DegradationStateUpdateSource, SourceDegradationState> sourceStates =
          sourceStatesById.getOrDefault(
              degradationId, new EnumMap<>(DegradationStateUpdateSource.class));

      EffectiveDegradationState effectiveState = resolveEffectiveState(sourceStates);
      if (effectiveState == null) {
        missingCount++;
        log.trace("No effective degradation state found: degradationId={}", degradationId);
        continue;
      }

      List<SourceDegradationState> orderedSourceStates =
          DegradationStateUpdateSource.resolutionOrder().stream()
              .map(sourceStates::get)
              .filter(java.util.Objects::nonNull)
              .toList();

      result.add(new AdminDegradationState(degradationId, effectiveState, orderedSourceStates));

      log.trace(
          "Admin degradation state resolved: degradationId={}, effectiveSource={}, effectiveValue={}, sourceStatesCount={}",
          degradationId,
          effectiveState.source(),
          effectiveState.value(),
          orderedSourceStates.size());
    }

    log.debug(
        "Admin degradation state lookup completed: requested={}, resolved={}, missing={}",
        degradationIds.size(),
        result.size(),
        missingCount);

    return result;
  }

  @SuppressWarnings("unchecked")
  public void updateDegradationStates(List<DegradationStateUpdate> updates) {
    if (CollectionUtils.isEmpty(updates)) {
      log.debug("Skipping degradation state update: empty updates");
      return;
    }

    log.info("Updating degradation states started: updatesCount={}", updates.size());

    redisTemplate.execute(
        new SessionCallback<>() {
          @Override
          public Object execute(RedisOperations operations) throws DataAccessException {
            operations.multi();
            try {
              for (DegradationStateUpdate update : updates) {
                String key = createStateKey(update.degradationId(), update.source());
                Duration ttl = update.ttl();

                validateUpdate(update, key);

                DegradationState state = stateUpdateMapper.fromUpdateToState(update);
                String json;
                try {
                  json = objectMapper.writeValueAsString(state);
                } catch (RuntimeException e) {
                  log.error(
                      "Failed to serialize degradation state update: degradationId={}, source={}",
                      update.degradationId(),
                      update.source(),
                      e);
                  throw e;
                }

                log.trace(
                    "Preparing degradation state update: degradationId={}, source={}, value={}, ttl={}",
                    update.degradationId(),
                    update.source(),
                    update.value(),
                    ttl);

                operations.opsForValue().set(key, json, ttl);
              }
            } catch (RuntimeException e) {
              log.error(
                  "Updating degradation states failed, discarding Redis transaction: updatesCount={}",
                  updates.size(),
                  e);
              operations.discard();
              throw e;
            }

            Object execResult = operations.exec();
            log.info(
                "Updating degradation states completed: updatesCount={}, execResultType={}",
                updates.size(),
                execResult.getClass().getSimpleName());
            return execResult;
          }
        });
  }

  public void clearDegradationStates(
      List<String> degradationIds, DegradationStateUpdateSource source) {
    if (CollectionUtils.isEmpty(degradationIds)) {
      log.debug("Skipping degradation state clear: empty degradationIds, source={}", source);
      return;
    }

    validateDegradationIds(degradationIds);

    List<String> keys = degradationIds.stream().map(id -> createStateKey(id, source)).toList();

    log.info(
        "Clearing degradation states started: source={}, degradationCount={}, redisKeyCount={}",
        source,
        degradationIds.size(),
        keys.size());

    Long deleted = redisTemplate.delete(keys);

    log.info(
        "Clearing degradation states completed: source={}, requested={}, deleted={}",
        source,
        degradationIds.size(),
        deleted == null ? 0 : deleted);
  }

  private Map<String, Map<DegradationStateUpdateSource, SourceDegradationState>>
      loadSourceStatesByDegradationId(List<String> degradationIds) {
    int sourcesCount = DegradationStateUpdateSource.values().length;
    List<String> keys = new ArrayList<>(degradationIds.size() * sourcesCount);
    List<RedisLookupEntry> lookupEntries = new ArrayList<>(degradationIds.size() * sourcesCount);

    for (String degradationId : degradationIds) {
      for (DegradationStateUpdateSource source : DegradationStateUpdateSource.values()) {
        String key = createStateKey(degradationId, source);
        keys.add(key);
        lookupEntries.add(new RedisLookupEntry(degradationId, source, key));
      }
    }

    log.debug(
        "Loading degradation states from Redis started: degradationCount={}, redisKeyCount={}",
        degradationIds.size(),
        keys.size());

    List<@Nullable String> values = redisTemplate.opsForValue().multiGet(keys);
    if (CollectionUtils.isEmpty(values)) {
      log.debug(
          "No degradation states found in Redis: degradationCount={}, redisKeyCount={}",
          degradationIds.size(),
          keys.size());
      return Collections.emptyMap();
    }

    if (values.size() != keys.size()) {
      log.warn(
          "Unexpected Redis state lookup result size: expectedValueCount={}, actualValueCount={}",
          keys.size(),
          values.size());
    }

    Map<String, Map<DegradationStateUpdateSource, SourceDegradationState>> result =
        new LinkedHashMap<>();

    for (int i = 0; i < lookupEntries.size(); i++) {
      if (i >= values.size()) {
        RedisLookupEntry entry = lookupEntries.get(i);
        log.warn(
            "Skipping Redis state resolution because result is shorter than expected: degradationId={}, source={}, valueIndex={}, actualValueCount={}",
            entry.degradationId(),
            entry.source(),
            i,
            values.size());
        break;
      }

      String json = values.get(i);
      if (json == null) {
        continue;
      }

      RedisLookupEntry entry = lookupEntries.get(i);
      try {
        DegradationState state = objectMapper.readValue(json, DegradationState.class);
        var remainingTtl =
            readRemainingTtl(entry.key(), entry.degradationId(), entry.source());
        if (remainingTtl.isEmpty()) {
          continue;
        }
        Duration resolvedRemainingTtl = remainingTtl.get();

        SourceDegradationState sourceState =
            new SourceDegradationState(entry.source(), state.value(), resolvedRemainingTtl);

        result
            .computeIfAbsent(
                entry.degradationId(), ignored -> new EnumMap<>(DegradationStateUpdateSource.class))
            .put(entry.source(), sourceState);

        log.trace(
            "Loaded degradation state from Redis: degradationId={}, source={}, value={}, remainingTtl={}",
            entry.degradationId(),
            entry.source(),
            state.value(),
            resolvedRemainingTtl);
      } catch (RuntimeException e) {
        log.error(
            "Failed to deserialize degradation state: degradationId={}, source={}",
            entry.degradationId(),
            entry.source(),
            e);
        throw e;
      }
    }

    log.debug(
        "Loading degradation states from Redis completed: degradationCount={}, resolvedDegradationCount={}",
        degradationIds.size(),
        result.size());

    return result;
  }

  private @Nullable EffectiveDegradationState resolveEffectiveState(
      Map<DegradationStateUpdateSource, SourceDegradationState> sourceStates) {
    for (DegradationStateUpdateSource source : DegradationStateUpdateSource.resolutionOrder()) {
      SourceDegradationState sourceState = sourceStates.get(source);
      if (sourceState != null) {
        return new EffectiveDegradationState(sourceState.value(), source);
      }
    }
    return null;
  }

  private Optional<Duration> readRemainingTtl(
      String key, String degradationId, DegradationStateUpdateSource source) {
    Long ttlMillis = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
    if (ttlMillis == null) {
      log.warn(
          "Redis TTL lookup returned null: degradationId={}, source={}, key={}",
          degradationId,
          source,
          key);
      return Optional.empty();
    }
    if (ttlMillis == -2) {
      log.warn(
          "Redis key disappeared before TTL lookup completed: degradationId={}, source={}, key={}",
          degradationId,
          source,
          key);
      return Optional.empty();
    }
    if (ttlMillis == -1) {
      log.warn(
          "Redis key has no TTL although TTL is expected: degradationId={}, source={}, key={}",
          degradationId,
          source,
          key);
      return Optional.empty();
    }
    if (ttlMillis < 0) {
      log.warn(
          "Unexpected Redis TTL value: degradationId={}, source={}, key={}, ttlMillis={}",
          degradationId,
          source,
          key,
          ttlMillis);
      return Optional.of(Duration.ZERO);
    }
    return Optional.of(Duration.ofMillis(ttlMillis));
  }

  private static void validateUpdate(DegradationStateUpdate update, String key) {
    validateDegradationId(update.degradationId());
    Duration ttl = update.ttl();
    if (ttl.isZero() || ttl.isNegative()) {
      throw new IllegalArgumentException("TTL must be positive for key " + key);
    }
    if (update.value() < 0.0 || update.value() > 1.0) {
      throw new IllegalArgumentException(
          "Value must be in range [0.0, 1.0] for degradationId=" + update.degradationId());
    }
  }

  private static void validateDegradationIds(List<String> degradationIds) {
    for (String degradationId : degradationIds) {
      validateDegradationId(degradationId);
    }
  }

  private static void validateDegradationId(String degradationId) {
    if (degradationId.isBlank()) {
      throw new IllegalArgumentException("Degradation id must not be blank");
    }
  }

  private static String createStateKey(String degradationId, DegradationStateUpdateSource source) {
    return "janus:state:" + degradationId + ":" + source.name().toLowerCase();
  }

  private record RedisLookupEntry(
      String degradationId, DegradationStateUpdateSource source, String key) {}
}
