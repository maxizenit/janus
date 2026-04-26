package org.janus.evaluator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.janus.evaluator.client.leadership.LeadershipClient;
import org.janus.evaluator.client.leadership.LeadershipHandle;
import org.janus.evaluator.client.signal.SignalClient;
import org.janus.evaluator.client.statestore.StateStoreClient;
import org.janus.evaluator.configuration.properties.EvaluatorProperties;
import org.janus.evaluator.metrics.EvaluatorMetrics;
import org.janus.evaluator.model.RegisteredDegradation;
import org.janus.evaluator.model.snapshot.PolicySnapshot;
import org.janus.evaluator.model.snapshot.SignalSourceSnapshot;
import org.janus.evaluator.registry.DegradationRegistry;
import org.janus.evaluator.validation.SignalValueValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

  private static final String DEGRADATION_ID = "test-degradation";
  private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");
  private static final Duration EVALUATION_INTERVAL = Duration.ofSeconds(30);
  private static final Duration LEASE_DURATION = Duration.ofSeconds(10);
  private static final Duration EFFECTIVE_LEASE_DURATION = EVALUATION_INTERVAL;
  private static final Duration LEADERSHIP_RETRY_BACKOFF = Duration.ofSeconds(5);
  private static final Duration FAILURE_BACKOFF = Duration.ofSeconds(15);

  @Mock private DegradationRegistry registry;
  @Mock private SignalClient signalClient;
  @Mock private StateStoreClient stateStoreClient;
  @Mock private LeadershipClient leadershipClient;
  @Mock private SignalValueValidator signalValueValidator;
  @Mock private EvaluatorMetrics evaluatorMetrics;

  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private EvaluatorProperties properties;
  private EvaluationService evaluationService;

  @BeforeEach
  void setUp() {
    properties =
        new EvaluatorProperties(
            "instance-1",
            Duration.ofMinutes(1),
            LEASE_DURATION,
            LEADERSHIP_RETRY_BACKOFF,
            FAILURE_BACKOFF,
            4,
            100);

    evaluationService =
        new EvaluationService(
            registry,
            signalClient,
            stateStoreClient,
            leadershipClient,
            signalValueValidator,
            properties,
            evaluatorMetrics,
            clock);
  }

  @Test
  void evaluate_successfulEvaluation_updatesStateWithCorrectTtl() {
    var holder = new RegisteredDegradation(DEGRADATION_ID);
    var signalSource =
        new SignalSourceSnapshot(SignalSourceSnapshot.SignalSourceType.PROMETHEUS, "up");
    var policy = new PolicySnapshot(DEGRADATION_ID, EVALUATION_INTERVAL, signalSource, NOW);
    holder.replacePolicy(policy);

    when(registry.find(DEGRADATION_ID)).thenReturn(Optional.of(holder));

    var leadershipHandle = mock(LeadershipHandle.class);
    when(leadershipHandle.acquired()).thenReturn(true);
    when(leadershipClient.tryAcquire(DEGRADATION_ID, EFFECTIVE_LEASE_DURATION))
        .thenReturn(leadershipHandle);

    when(signalClient.getSignalValue(signalSource)).thenReturn(0.75);
    when(signalValueValidator.validate(0.75, DEGRADATION_ID)).thenReturn(0.75);

    var result = evaluationService.evaluate(DEGRADATION_ID);

    assertThat(result).isPresent();
    assertThat(result.get().degradationId()).isEqualTo(DEGRADATION_ID);
    assertThat(result.get().nextEvaluationAt()).isEqualTo(NOW.plus(EVALUATION_INTERVAL));

    Duration expectedTtl = EVALUATION_INTERVAL.multipliedBy(2);
    verify(stateStoreClient).updateState(DEGRADATION_ID, 0.75, expectedTtl);
    verify(evaluatorMetrics).recordLeadershipAcquisition(DEGRADATION_ID, "acquired");
    verify(evaluatorMetrics).recordEvaluation(DEGRADATION_ID, "success");
    verify(leadershipHandle, never()).release();
    verify(leadershipHandle).close();
  }

  @Test
  void evaluate_leadershipNotAcquired_skipsSignalFetchAndStateUpdate() {
    var holder = new RegisteredDegradation(DEGRADATION_ID);
    var signalSource =
        new SignalSourceSnapshot(SignalSourceSnapshot.SignalSourceType.PROMETHEUS, "up");
    var policy = new PolicySnapshot(DEGRADATION_ID, EVALUATION_INTERVAL, signalSource, NOW);
    holder.replacePolicy(policy);

    when(registry.find(DEGRADATION_ID)).thenReturn(Optional.of(holder));

    var leadershipHandle = mock(LeadershipHandle.class);
    when(leadershipHandle.acquired()).thenReturn(false);
    when(leadershipClient.tryAcquire(DEGRADATION_ID, EFFECTIVE_LEASE_DURATION))
        .thenReturn(leadershipHandle);

    var result = evaluationService.evaluate(DEGRADATION_ID);

    assertThat(result).isPresent();
    assertThat(result.get().degradationId()).isEqualTo(DEGRADATION_ID);
    assertThat(result.get().nextEvaluationAt()).isEqualTo(NOW.plus(EVALUATION_INTERVAL));

    verifyNoInteractions(signalClient);
    verifyNoInteractions(stateStoreClient);
    verify(evaluatorMetrics).recordLeadershipAcquisition(DEGRADATION_ID, "rejected");
    verify(evaluatorMetrics, never()).recordEvaluation(anyString(), anyString());
    verify(leadershipHandle, never()).release();
    verify(leadershipHandle).close();
  }

  @Test
  void evaluate_leadershipNotAcquired_usesRetryBackoffWhenItExceedsEvaluationInterval() {
    var holder = new RegisteredDegradation(DEGRADATION_ID);
    var signalSource =
        new SignalSourceSnapshot(SignalSourceSnapshot.SignalSourceType.PROMETHEUS, "up");
    var policy =
        new PolicySnapshot(
            DEGRADATION_ID, LEADERSHIP_RETRY_BACKOFF.minusSeconds(1), signalSource, NOW);
    holder.replacePolicy(policy);

    when(registry.find(DEGRADATION_ID)).thenReturn(Optional.of(holder));

    var leadershipHandle = mock(LeadershipHandle.class);
    when(leadershipHandle.acquired()).thenReturn(false);
    when(leadershipClient.tryAcquire(DEGRADATION_ID, LEASE_DURATION)).thenReturn(leadershipHandle);

    var result = evaluationService.evaluate(DEGRADATION_ID);

    assertThat(result).isPresent();
    assertThat(result.get().nextEvaluationAt()).isEqualTo(NOW.plus(LEADERSHIP_RETRY_BACKOFF));

    verifyNoInteractions(signalClient);
    verifyNoInteractions(stateStoreClient);
    verify(evaluatorMetrics).recordLeadershipAcquisition(DEGRADATION_ID, "rejected");
    verify(evaluatorMetrics, never()).recordEvaluation(anyString(), anyString());
    verify(leadershipHandle, never()).release();
    verify(leadershipHandle).close();
  }

  @Test
  void evaluate_signalFetchFailure_handledGracefully() {
    var holder = new RegisteredDegradation(DEGRADATION_ID);
    var signalSource =
        new SignalSourceSnapshot(SignalSourceSnapshot.SignalSourceType.PROMETHEUS, "up");
    var policy = new PolicySnapshot(DEGRADATION_ID, EVALUATION_INTERVAL, signalSource, NOW);
    holder.replacePolicy(policy);

    when(registry.find(DEGRADATION_ID)).thenReturn(Optional.of(holder));

    var leadershipHandle = mock(LeadershipHandle.class);
    when(leadershipHandle.acquired()).thenReturn(true);
    when(leadershipClient.tryAcquire(DEGRADATION_ID, EFFECTIVE_LEASE_DURATION))
        .thenReturn(leadershipHandle);

    when(signalClient.getSignalValue(signalSource))
        .thenThrow(new RuntimeException("Prometheus unavailable"));

    var result = evaluationService.evaluate(DEGRADATION_ID);

    assertThat(result).isPresent();
    assertThat(result.get().degradationId()).isEqualTo(DEGRADATION_ID);
    assertThat(result.get().nextEvaluationAt()).isEqualTo(NOW.plus(FAILURE_BACKOFF));

    verify(stateStoreClient, never()).updateState(anyString(), anyDouble(), any());
    verify(evaluatorMetrics).recordEvaluation(DEGRADATION_ID, "failure");
    verify(leadershipHandle).release();
    verify(leadershipHandle).close();
  }

  @Test
  void evaluate_degradationNotRegistered_returnsEmpty() {
    when(registry.find(DEGRADATION_ID)).thenReturn(Optional.empty());

    var result = evaluationService.evaluate(DEGRADATION_ID);

    assertThat(result).isEmpty();
    verifyNoInteractions(leadershipClient);
    verifyNoInteractions(signalClient);
    verifyNoInteractions(stateStoreClient);
  }

  @Test
  void evaluate_evaluationAlreadyRunning_returnsEmpty() {
    var holder = new RegisteredDegradation(DEGRADATION_ID);
    holder.tryStartEvaluation(); // mark as running

    when(registry.find(DEGRADATION_ID)).thenReturn(Optional.of(holder));

    var result = evaluationService.evaluate(DEGRADATION_ID);

    assertThat(result).isEmpty();
    verifyNoInteractions(leadershipClient);
    verifyNoInteractions(signalClient);
    verifyNoInteractions(stateStoreClient);
  }

  @Test
  void evaluate_holderInactive_returnsEmpty() {
    var holder = new RegisteredDegradation(DEGRADATION_ID);
    holder.deactivate();

    when(registry.find(DEGRADATION_ID)).thenReturn(Optional.of(holder));

    var result = evaluationService.evaluate(DEGRADATION_ID);

    assertThat(result).isEmpty();
    verifyNoInteractions(leadershipClient);
  }

  @Test
  void evaluate_policyMissing_returnsEmpty() {
    var holder = new RegisteredDegradation(DEGRADATION_ID);
    // no policy set

    when(registry.find(DEGRADATION_ID)).thenReturn(Optional.of(holder));

    var result = evaluationService.evaluate(DEGRADATION_ID);

    assertThat(result).isEmpty();
    verifyNoInteractions(leadershipClient);
  }

  @Test
  void evaluate_stateUpdateWithTtlTwiceEvaluationInterval() {
    var holder = new RegisteredDegradation(DEGRADATION_ID);
    var interval = Duration.ofMinutes(5);
    var signalSource =
        new SignalSourceSnapshot(SignalSourceSnapshot.SignalSourceType.PROMETHEUS, "metric");
    var policy = new PolicySnapshot(DEGRADATION_ID, interval, signalSource, NOW);
    holder.replacePolicy(policy);

    when(registry.find(DEGRADATION_ID)).thenReturn(Optional.of(holder));

    var leadershipHandle = mock(LeadershipHandle.class);
    when(leadershipHandle.acquired()).thenReturn(true);
    when(leadershipClient.tryAcquire(DEGRADATION_ID, interval)).thenReturn(leadershipHandle);

    when(signalClient.getSignalValue(signalSource)).thenReturn(0.5);
    when(signalValueValidator.validate(0.5, DEGRADATION_ID)).thenReturn(0.5);

    evaluationService.evaluate(DEGRADATION_ID);

    verify(stateStoreClient).updateState(DEGRADATION_ID, 0.5, Duration.ofMinutes(10));
  }

  @Test
  void evaluate_validationFailure_treatedAsRuntimeException() {
    var holder = new RegisteredDegradation(DEGRADATION_ID);
    var signalSource =
        new SignalSourceSnapshot(SignalSourceSnapshot.SignalSourceType.PROMETHEUS, "up");
    var policy = new PolicySnapshot(DEGRADATION_ID, EVALUATION_INTERVAL, signalSource, NOW);
    holder.replacePolicy(policy);

    when(registry.find(DEGRADATION_ID)).thenReturn(Optional.of(holder));

    var leadershipHandle = mock(LeadershipHandle.class);
    when(leadershipHandle.acquired()).thenReturn(true);
    when(leadershipClient.tryAcquire(DEGRADATION_ID, EFFECTIVE_LEASE_DURATION))
        .thenReturn(leadershipHandle);

    when(signalClient.getSignalValue(signalSource)).thenReturn(-1.0);
    when(signalValueValidator.validate(-1.0, DEGRADATION_ID))
        .thenThrow(new IllegalArgumentException("out of range"));

    var result = evaluationService.evaluate(DEGRADATION_ID);

    assertThat(result).isPresent();
    assertThat(result.get().nextEvaluationAt()).isEqualTo(NOW.plus(FAILURE_BACKOFF));
    verify(stateStoreClient, never()).updateState(anyString(), anyDouble(), any());
    verify(evaluatorMetrics).recordEvaluation(DEGRADATION_ID, "failure");
    verify(leadershipHandle).release();
  }
}
