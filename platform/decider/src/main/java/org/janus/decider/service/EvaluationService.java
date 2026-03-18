package org.janus.decider.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.decider.client.leadership.LeadershipClient;
import org.janus.decider.client.signal.SignalClient;
import org.janus.decider.client.statestore.StateStoreClient;
import org.janus.decider.configuration.properties.DeciderProperties;
import org.janus.decider.model.EvaluationResult;
import org.janus.decider.registry.DegradationRegistry;
import org.janus.decider.validation.SignalValueValidator;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class EvaluationService {

  private final DegradationRegistry registry;
  private final SignalClient signalClient;
  private final StateStoreClient stateStoreClient;
  private final LeadershipClient leadershipClient;
  private final SignalValueValidator signalValueValidator;
  private final DeciderProperties properties;
  private final Clock clock;

  public Optional<EvaluationResult> evaluate(String degradationId) {
    var holder = registry.find(degradationId).orElse(null);
    if (holder == null || !holder.tryStartEvaluation()) {
      return Optional.empty();
    }

    try {
      if (!holder.isActive()) {
        return Optional.empty();
      }

      var policy = holder.getPolicy().orElse(null);
      if (policy == null) {
        return Optional.empty();
      }

      try (var leadership =
          leadershipClient.tryAcquire(
              policy.degradationId(), properties.leadershipLeaseDuration())) {

        if (!leadership.acquired()) {
          var nextAttemptAt = Instant.now(clock).plus(properties.leadershipRetryBackoff());
          holder.setNextEvaluationAt(nextAttemptAt);

          log.debug("Leadership not acquired: degradation={}", policy.degradationId());
          return Optional.of(new EvaluationResult(policy.degradationId(), nextAttemptAt));
        }

        var rawValue =
            signalClient.getSignalValue(policy.signalSource(), policy.evaluationInterval());
        var value = signalValueValidator.validate(rawValue, policy.degradationId());

        stateStoreClient.updateState(
            policy.degradationId(), value, policy.evaluationInterval().multipliedBy(2));

        var nextEvaluationAt = Instant.now(clock).plus(policy.evaluationInterval());
        holder.setNextEvaluationAt(nextEvaluationAt);

        log.debug("Degradation evaluated: degradation={}, value={}", policy.degradationId(), value);

        return Optional.of(new EvaluationResult(policy.degradationId(), nextEvaluationAt));
      }
    } catch (RuntimeException e) {
      var retryAt = Instant.now(clock).plus(properties.evaluationFailureBackoff());
      holder.setNextEvaluationAt(retryAt);

      log.warn("Evaluation failed: degradation={}", degradationId, e);
      return Optional.of(new EvaluationResult(degradationId, retryAt));
    } finally {
      holder.finishEvaluation();
    }
  }
}
