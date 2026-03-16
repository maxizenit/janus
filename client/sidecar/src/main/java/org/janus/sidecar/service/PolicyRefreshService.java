package org.janus.sidecar.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.sidecar.client.policystore.PolicyStoreClient;
import org.janus.sidecar.model.RegisteredDegradation;
import org.janus.sidecar.registry.ActualDegradationRegistry;
import org.janus.sidecar.scheduling.StateRefreshScheduler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class PolicyRefreshService {

  private final ActualDegradationRegistry registry;
  private final PolicyStoreClient policyStoreClient;
  private final StateRefreshScheduler stateRefreshScheduler;
  private final Clock clock;

  public void refreshAllRegisteredPolicies() {
    var ids =
        registry.findAllActive().stream()
            .map(RegisteredDegradation::getDegradationId)
            .collect(Collectors.toSet());
    refreshPolicies(ids);
  }

  public Set<String> refreshPolicies(Set<String> degradationIds) {
    if (degradationIds.isEmpty()) {
      return Collections.emptySet();
    }
    log.debug("Refreshing policies for {} degradations", degradationIds.size());

    var policies = policyStoreClient.getPolicies(degradationIds);
    var now = Instant.now(clock);
    var refreshedIds = new HashSet<String>();

    for (var entry : policies.entrySet()) {
      registry
          .find(entry.getKey())
          .ifPresent(
              holder -> {
                if (!holder.isActive()) {
                  return;
                }

                var oldPolicy = holder.getPolicy().orElse(null);
                var newPolicy = entry.getValue();

                holder.replacePolicy(newPolicy);
                refreshedIds.add(holder.getDegradationId());
                log.debug(
                    "Policy updated: degradation={}, evaluationInterval={}",
                    holder.getDegradationId(),
                    newPolicy.evaluationInterval());

                var evaluationChanged =
                    oldPolicy == null
                        || !Objects.equals(
                            oldPolicy.evaluationInterval(), newPolicy.evaluationInterval());

                if (evaluationChanged) {
                  holder.setNextStateRefreshAt(now);
                  stateRefreshScheduler.scheduleNow(holder.getDegradationId());
                  log.info(
                      "Evaluation interval changed: degradation={}, old={}, new={}",
                      holder.getDegradationId(),
                      oldPolicy != null ? oldPolicy.evaluationInterval() : null,
                      newPolicy.evaluationInterval());
                }
              });
    }

    return Set.copyOf(refreshedIds);
  }
}
