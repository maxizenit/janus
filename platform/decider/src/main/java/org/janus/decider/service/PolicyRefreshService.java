package org.janus.decider.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
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
    log.info("Refreshing decider policies started");

    var policies = policyStoreClient.getAllPolicies();
    log.debug("Policies fetched from policy store: count={}", policies.size());

    if (policies.isEmpty()) {
      log.warn("Policy refresh returned no policies");
    }

    var desiredIds = policies.keySet();
    registry.sync(desiredIds);

    var registeredCount = desiredIds.size();

    var now = Instant.now(clock);
    var updatedCount = new AtomicInteger();
    var unchangedCount = new AtomicInteger();

    policies.forEach(
        (degradationId, newPolicy) -> {
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

                    var evaluationChanged =
                        oldPolicy == null
                            || !Objects.equals(
                                oldPolicy.evaluationInterval(), newPolicy.evaluationInterval())
                            || !Objects.equals(oldPolicy.signalSource(), newPolicy.signalSource());

                    if (evaluationChanged) {
                      updatedCount.incrementAndGet();
                      holder.setNextEvaluationAt(now);
                      evaluationScheduler.scheduleNow(holder.getDegradationId());

                      log.info(
                          "Policy updated: degradation={}, evaluationInterval={}, signalSourceType={}, scheduledAt={}",
                          holder.getDegradationId(),
                          newPolicy.evaluationInterval(),
                          newPolicy.signalSource().type(),
                          now);
                    } else {
                      unchangedCount.incrementAndGet();
                      log.debug(
                          "Policy unchanged: degradation={}, evaluationInterval={}, signalSourceType={}",
                          holder.getDegradationId(),
                          newPolicy.evaluationInterval(),
                          newPolicy.signalSource().type());
                    }
                  });
        });

    log.info(
        "Refreshing decider policies completed: total={}, registered={}, updated={}, unchanged={}",
        policies.size(),
        updatedCount.get(),
        unchangedCount.get(),
        unchangedCount.get());
  }
}
