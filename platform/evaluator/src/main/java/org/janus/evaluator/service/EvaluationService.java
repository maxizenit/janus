package org.janus.evaluator.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.evaluator.client.leadership.LeadershipClient;
import org.janus.evaluator.client.signal.SignalClient;
import org.janus.evaluator.client.statestore.StateStoreClient;
import org.janus.evaluator.configuration.properties.EvaluatorProperties;
import org.janus.evaluator.metrics.EvaluatorMetrics;
import org.janus.evaluator.model.EvaluationResult;
import org.janus.evaluator.registry.DegradationRegistry;
import org.janus.evaluator.validation.SignalValueValidator;
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
  private final EvaluatorProperties properties;
  private final EvaluatorMetrics evaluatorMetrics;
  private final Clock clock;

  public Optional<EvaluationResult> evaluate(String degradationId) {
    var holder = registry.find(degradationId).orElse(null);
    if (holder == null) {
      log.debug("Evaluation skipped: degradation={}, reason=not_registered", degradationId);
      return Optional.empty();
    }

    if (!holder.tryStartEvaluation()) {
      log.debug("Evaluation skipped: degradation={}, reason=already_running", degradationId);
      return Optional.empty();
    }

    try {
      if (!holder.isActive()) {
        log.debug("Evaluation skipped: degradation={}, reason=inactive", degradationId);
        return Optional.empty();
      }

      var policy = holder.getPolicy().orElse(null);
      if (policy == null) {
        log.debug("Evaluation skipped: degradation={}, reason=policy_missing", degradationId);
        return Optional.empty();
      }

      log.debug(
          "Evaluation started: degradation={}, evaluationInterval={}, signalSourceType={}, signalSourceRef={}",
          policy.degradationId(),
          policy.evaluationInterval(),
          policy.signalSource().type(),
          policy.signalSource().reference());

      try (var leadership =
          leadershipClient.tryAcquire(
              policy.degradationId(), properties.leadershipLeaseDuration())) {

        if (!leadership.acquired()) {
          evaluatorMetrics.recordLeadershipAcquisition(policy.degradationId(), "rejected");

          var nextAttemptAt = Instant.now(clock).plus(properties.leadershipRetryBackoff());
          holder.setNextEvaluationAt(nextAttemptAt);

          log.debug(
              "Leadership not acquired: degradation={}, nextAttemptAt={}, leaseDuration={}",
              policy.degradationId(),
              nextAttemptAt,
              properties.leadershipLeaseDuration());
          return Optional.of(new EvaluationResult(policy.degradationId(), nextAttemptAt));
        }

        evaluatorMetrics.recordLeadershipAcquisition(policy.degradationId(), "acquired");

        log.debug(
            "Leadership acquired: degradation={}, leaseDuration={}, instanceId={}",
            policy.degradationId(),
            properties.leadershipLeaseDuration(),
            properties.instanceId());

        var signalFetchStart = Instant.now(clock);
        var rawValue =
            signalClient.getSignalValue(policy.signalSource(), policy.evaluationInterval());
        evaluatorMetrics.recordSignalFetchDuration(
            policy.degradationId(), Duration.between(signalFetchStart, Instant.now(clock)));

        log.debug(
            "Signal value received: degradation={}, rawValue={}, signalSourceType={}, signalSourceRef={}",
            policy.degradationId(),
            rawValue,
            policy.signalSource().type(),
            policy.signalSource().reference());

        var value = signalValueValidator.validate(rawValue, policy.degradationId());
        log.debug(
            "Signal value validated: degradation={}, rawValue={}, validatedValue={}",
            policy.degradationId(),
            rawValue,
            value);

        Duration ttl = policy.evaluationInterval().multipliedBy(2);
        stateStoreClient.updateState(policy.degradationId(), value, ttl);
        log.debug(
            "State store updated: degradation={}, value={}, ttl={}",
            policy.degradationId(),
            value,
            ttl);

        var nextEvaluationAt = Instant.now(clock).plus(policy.evaluationInterval());
        holder.setNextEvaluationAt(nextEvaluationAt);
        log.debug(
            "Degradation evaluated: degradation={}, value={}, nextEvaluationAt={}",
            policy.degradationId(),
            value,
            nextEvaluationAt);

        evaluatorMetrics.recordEvaluation(policy.degradationId(), "success");
        return Optional.of(new EvaluationResult(policy.degradationId(), nextEvaluationAt));
      }
    } catch (RuntimeException e) {
      var retryAt = Instant.now(clock).plus(properties.evaluationFailureBackoff());
      holder.setNextEvaluationAt(retryAt);

      evaluatorMetrics.recordEvaluation(degradationId, "failure");

      log.warn(
          "Evaluation failed: degradation={}, retryAt={}, failureBackoff={}",
          degradationId,
          retryAt,
          properties.evaluationFailureBackoff(),
          e);
      return Optional.of(new EvaluationResult(degradationId, retryAt));
    } finally {
      holder.finishEvaluation();
      log.trace("Evaluation finished: degradation={}", degradationId);
    }
  }
}
