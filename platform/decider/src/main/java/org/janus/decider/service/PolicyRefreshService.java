package org.janus.decider.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.decider.client.policystore.PolicyStoreClient;
import org.janus.decider.registry.DegradationRegistry;
import org.janus.decider.scheduling.EvaluationScheduler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class PolicyRefreshService {

  private final DegradationRegistry registry;
  private final PolicyStoreClient policyStoreClient;
  private final EvaluationScheduler evaluationScheduler;
  private final Clock clock;

  public void refreshAllPolicies() {
    var policies = policyStoreClient.getAllPolicies();
    var desiredIds = policies.keySet();

    registry.sync(desiredIds);

    var now = Instant.now(clock);

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

                var evaluationChanged =
                    oldPolicy == null
                        || !Objects.equals(
                            oldPolicy.evaluationInterval(), newPolicy.evaluationInterval())
                        || !Objects.equals(oldPolicy.signalSource(), newPolicy.signalSource());

                if (evaluationChanged) {
                  holder.setNextEvaluationAt(now);
                  evaluationScheduler.scheduleNow(holder.getDegradationId());

                  log.info(
                      "Policy updated: degradation={}, evaluationInterval={}, signalSourceType={}",
                      holder.getDegradationId(),
                      newPolicy.evaluationInterval(),
                      newPolicy.signalSource().type());
                }
              });
    }
  }
}
