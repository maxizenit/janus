package org.janus.sidecar.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.sidecar.client.statestore.StateStoreClient;
import org.janus.sidecar.model.RegisteredDegradation;
import org.janus.sidecar.model.StateRefreshResult;
import org.janus.sidecar.registry.ActualDegradationRegistry;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class StateRefreshService {

  private final ActualDegradationRegistry registry;
  private final StateStoreClient stateStoreClient;
  private final Clock clock;

  public List<StateRefreshResult> refresh(Set<String> degradationIds) {
    if (degradationIds.isEmpty()) {
      log.debug("Skipping state refresh: empty degradationIds");
      return Collections.emptyList();
    }

    var eligible =
        degradationIds.stream()
            .map(registry::find)
            .flatMap(Optional::stream)
            .filter(RegisteredDegradation::isActive)
            .filter(RegisteredDegradation::tryStartStateRefresh)
            .toList();

    if (eligible.isEmpty()) {
      log.debug(
          "Skipping state refresh: no eligible degradations, requested={}", degradationIds.size());
      return Collections.emptyList();
    }

    try {
      var ids =
          eligible.stream()
              .map(RegisteredDegradation::getDegradationId)
              .collect(Collectors.toSet());
      log.debug(
          "Refreshing states started: requested={}, eligible={}",
          degradationIds.size(),
          ids.size());

      var states = stateStoreClient.getStates(ids);
      var now = Instant.now(clock);
      var results = new ArrayList<StateRefreshResult>(eligible.size());
      var updatedCount = new AtomicInteger();
      var missingCount = new AtomicInteger();

      log.debug(
          "States fetched from state store: eligible={}, returned={}", ids.size(), states.size());

      for (var holder : eligible) {
        try {
          if (!holder.isActive()) {
            continue;
          }

          var state = states.get(holder.getDegradationId());
          if (state != null) {
            holder.setState(state);
            updatedCount.incrementAndGet();
          } else {
            missingCount.incrementAndGet();
            holder.markStateStale(now);
            log.debug("State not found in state store: degradation={}", holder.getDegradationId());
          }

          holder
              .getPolicy()
              .ifPresent(
                  policy -> {
                    var nextRefreshAt = now.plus(policy.evaluationInterval());
                    results.add(new StateRefreshResult(holder.getDegradationId(), nextRefreshAt));
                    log.debug(
                        "State refreshed: degradation={}, value={}, nextRefreshAt={}",
                        holder.getDegradationId(),
                        state != null ? state.value() : null,
                        nextRefreshAt);
                  });

        } finally {
          holder.finishStateRefresh();
        }
      }

      log.info(
          "Refreshing states completed: requested={}, eligible={}, updated={}, missing={}, rescheduled={}",
          degradationIds.size(),
          eligible.size(),
          updatedCount.get(),
          missingCount.get(),
          results.size());

      return List.copyOf(results);
    } catch (Exception e) {
      var now = Instant.now(clock);
      int staleMarked = 0;
      var results = new ArrayList<StateRefreshResult>(eligible.size());

      for (var holder : eligible) {
        try {
          if (holder.markStateStale(now)) {
            staleMarked++;
          }
          log.warn(
              "State refresh failed, marking stale: degradation={}", holder.getDegradationId());
          holder
              .getPolicy()
              .ifPresent(
                  policy -> {
                    var nextRefreshAt = now.plus(policy.evaluationInterval());
                    results.add(new StateRefreshResult(holder.getDegradationId(), nextRefreshAt));
                  });
        } finally {
          holder.finishStateRefresh();
        }
      }

      log.warn(
          "Refreshing states failed: requested={}, eligible={}, staleMarked={}, rescheduled={}",
          degradationIds.size(),
          eligible.size(),
          staleMarked,
          results.size(),
          e);

      return List.copyOf(results);
    }
  }
}
