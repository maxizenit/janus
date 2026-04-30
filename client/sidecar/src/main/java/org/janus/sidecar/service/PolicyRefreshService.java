package org.janus.sidecar.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
      log.debug("Skipping policy refresh: empty degradationIds");
      return Collections.emptySet();
    }

    log.info("Refreshing policies started: requested={}", degradationIds.size());

    var policies = policyStoreClient.getPolicies(degradationIds);
    var now = Instant.now(clock);
    var refreshedIds = new HashSet<String>();
    var updatedCount = new AtomicInteger();
    var unchangedCount = new AtomicInteger();

    var missingIds = new HashSet<>(degradationIds);
    missingIds.removeAll(policies.keySet());

    log.debug(
        "Policies fetched from policy store: requested={}, received={}",
        degradationIds.size(),
        policies.size());

    if (!missingIds.isEmpty()) {
      missingIds.forEach(
          missingId ->
              registry
                  .find(missingId)
                  .ifPresent(
                      holder -> {
                        holder.clearPolicy();
                        holder.clearState();
                      }));
      log.warn(
          "Policies missing in policy store response: missingCount={}, missingIds={}",
          missingIds.size(),
          missingIds.size() <= 20 ? missingIds : "[omitted]");
    }

    policies.forEach(
        (degradationId, newPolicy) ->
            registry
                .find(degradationId)
                .ifPresent(
                    holder -> {
                      if (!holder.isActive()) {
                        log.debug(
                            "Skipping policy refresh for inactive degradation: degradation={}",
                            holder.getDegradationId());
                        return;
                      }

                      var oldPolicy = holder.getPolicy().orElse(null);

                      holder.replacePolicy(newPolicy);
                      refreshedIds.add(holder.getDegradationId());

                      var evaluationChanged =
                          oldPolicy == null
                              || !Objects.equals(
                                  oldPolicy.evaluationInterval(), newPolicy.evaluationInterval());

                      if (evaluationChanged) {
                        updatedCount.incrementAndGet();
                        stateRefreshScheduler.scheduleNow(holder.getDegradationId());
                        log.info(
                            "Policy refreshed: degradation={}, oldEvaluationInterval={}, newEvaluationInterval={}, scheduledAt={}",
                            holder.getDegradationId(),
                            oldPolicy != null ? oldPolicy.evaluationInterval() : null,
                            newPolicy.evaluationInterval(),
                            now);
                      } else {
                        unchangedCount.incrementAndGet();
                        log.debug(
                            "Policy unchanged: degradation={}, evaluationInterval={}",
                            holder.getDegradationId(),
                            newPolicy.evaluationInterval());
                      }
                    }));

    log.info(
        "Refreshing policies completed: requested={}, refreshed={}, updated={}, unchanged={}, missing={}",
        degradationIds.size(),
        refreshedIds.size(),
        updatedCount.get(),
        unchangedCount.get(),
        missingIds.size());

    return Set.copyOf(refreshedIds);
  }
}
