package org.janus.statestore.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.statestore.mapper.DegradationStateUpdateMapper;
import org.janus.statestore.model.DegradationState;
import org.janus.statestore.model.DegradationStateUpdate;
import org.janus.statestore.model.DegradationStateUpdateSource;
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
    if (CollectionUtils.isEmpty(degradationIds)) {
      log.debug("Skipping degradation state lookup: empty degradationIds");
      return Collections.emptyList();
    }

    int sourcesCount = DegradationStateUpdateSource.values().length;
    List<String> keys = new ArrayList<>(degradationIds.size() * sourcesCount);
    for (String id : degradationIds) {
      for (DegradationStateUpdateSource source : DegradationStateUpdateSource.values()) {
        keys.add(createStateKey(id, source));
      }
    }

    log.debug(
        "Loading degradation states from Redis: degradationCount={}, redisKeyCount={}",
        degradationIds.size(),
        keys.size());

    List<@Nullable String> values = redisTemplate.opsForValue().multiGet(keys);
    if (CollectionUtils.isEmpty(values)) {
      log.debug(
          "No degradation states found in Redis: degradationCount={}, redisKeyCount={}",
          degradationIds.size(),
          keys.size());
      return Collections.emptyList();
    }

    log.debug(
        "Redis state lookup completed: degradationCount={}, returnedValueCount={}",
        degradationIds.size(),
        values.size());

    if (values.size() != keys.size()) {
      log.warn(
          "Unexpected Redis state lookup result size: expectedValueCount={}, actualValueCount={}",
          keys.size(),
          values.size());
    }

    Map<String, DegradationState> resolved = new LinkedHashMap<>();
    for (int i = 0; i < degradationIds.size(); ++i) {
      String id = degradationIds.get(i);
      boolean stateResolved = false;
      for (int j = 0; j < sourcesCount; ++j) {
        int valueIndex = i * sourcesCount + j;
        if (valueIndex >= values.size()) {
          log.warn(
              "Skipping degradation state resolution because Redis result is shorter than expected: degradationId={}, valueIndex={}, actualValueCount={}",
              id,
              valueIndex,
              values.size());
          break;
        }

        DegradationStateUpdateSource source = DegradationStateUpdateSource.values()[j];
        String json = values.get(i * sourcesCount + j);
        if (json != null) {
          try {
            DegradationState state = objectMapper.readValue(json, DegradationState.class);
            resolved.put(id, state);
            stateResolved = true;
            log.trace(
                "Resolved degradation state: degradationId={}, source={}, value={}",
                id,
                source,
                state.value());
          } catch (RuntimeException e) {
            log.error(
                "Failed to deserialize degradation state: degradationId={}, source={}",
                id,
                source,
                e);
            throw e;
          }
          break;
        }
      }

      if (!stateResolved) {
        log.trace("Degradation state not found for any source: degradationId={}", id);
      }
    }

    log.debug(
        "Degradation state lookup completed: requested={}, resolved={}, missing={}",
        degradationIds.size(),
        resolved.size(),
        degradationIds.size() - resolved.size());

    return new ArrayList<>(resolved.values());
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
                if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                  log.warn(
                      "Invalid TTL for degradation state update: degradationId={}, source={}, ttl={}",
                      update.degradationId(),
                      update.source(),
                      ttl);
                  throw new IllegalArgumentException("TTL must be positive for key " + key);
                }

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

  private static String createStateKey(String degradationId, DegradationStateUpdateSource source) {
    return "janus:state:" + degradationId + ":" + source.name().toLowerCase();
  }
}
