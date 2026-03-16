package org.janus.sidecar.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    var eligible =
        degradationIds.stream()
            .map(registry::find)
            .flatMap(Optional::stream)
            .filter(RegisteredDegradation::isActive)
            .filter(RegisteredDegradation::tryStartStateRefresh)
            .toList();

    if (eligible.isEmpty()) {
      return Collections.emptyList();
    }

    try {
      var ids =
          eligible.stream()
              .map(RegisteredDegradation::getDegradationId)
              .collect(Collectors.toSet());
      log.debug("Refreshing states for {} degradations", ids.size());

      var states = stateStoreClient.getStates(ids);
      var now = Instant.now(clock);
      var results = new ArrayList<StateRefreshResult>(eligible.size());

      for (var holder : eligible) {
        try {
          if (!holder.isActive()) {
            continue;
          }

          var state = states.get(holder.getDegradationId());
          if (state != null) {
            holder.setState(state);
          }

          holder
              .getPolicy()
              .ifPresent(
                  policy -> {
                    var nextRefreshAt = now.plus(policy.evaluationInterval());
                    holder.setNextStateRefreshAt(nextRefreshAt);
                    results.add(new StateRefreshResult(holder.getDegradationId(), nextRefreshAt));
                    log.debug(
                        "State updated: degradation={}, value={}",
                        holder.getDegradationId(),
                        state != null ? state.value() : null);
                  });

        } finally {
          holder.finishStateRefresh();
        }
      }

      return List.copyOf(results);
    } catch (Exception e) {
      var now = Instant.now(clock);

      for (var holder : eligible) {
        try {
          holder.getState().ifPresent(previous -> holder.setState(previous.staleCopy(now)));
          log.warn(
              "State refresh failed, marking stale: degradation={}", holder.getDegradationId());
        } finally {
          holder.finishStateRefresh();
        }
      }

      throw e;
    }
  }
}
