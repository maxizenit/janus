package org.janus.evaluator.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.janus.evaluator.client.policystore.PolicyStoreClient;
import org.janus.evaluator.model.RegisteredDegradation;
import org.janus.evaluator.model.snapshot.PolicySnapshot;
import org.janus.evaluator.model.snapshot.SignalSourceSnapshot;
import org.janus.evaluator.registry.DegradationRegistry;
import org.janus.evaluator.scheduling.EvaluationScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyRefreshServiceTest {

  private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");
  private static final String DEGRADATION_ID = "test-degradation";

  @Mock private DegradationRegistry registry;
  @Mock private PolicyStoreClient policyStoreClient;
  @Mock private EvaluationScheduler evaluationScheduler;

  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private PolicyRefreshService policyRefreshService;

  @BeforeEach
  void setUp() {
    policyRefreshService =
        new PolicyRefreshService(registry, policyStoreClient, evaluationScheduler, clock);
  }

  @Test
  void refreshAllPolicies_newPolicy_syncsRegistryAndSchedulesEvaluation() {
    var signalSource =
        new SignalSourceSnapshot(SignalSourceSnapshot.SignalSourceType.PROMETHEUS, "up");
    var policy =
        new PolicySnapshot(DEGRADATION_ID, Duration.ofSeconds(30), signalSource, NOW);

    when(policyStoreClient.getAllPolicies()).thenReturn(Map.of(DEGRADATION_ID, policy));

    var holder = new RegisteredDegradation(DEGRADATION_ID);
    when(registry.find(DEGRADATION_ID)).thenReturn(Optional.of(holder));

    policyRefreshService.refreshAllPolicies();

    verify(registry).sync(Set.of(DEGRADATION_ID));
    verify(evaluationScheduler).scheduleNow(DEGRADATION_ID);
  }

  @Test
  void refreshAllPolicies_unchangedPolicy_doesNotReschedule() {
    var signalSource =
        new SignalSourceSnapshot(SignalSourceSnapshot.SignalSourceType.PROMETHEUS, "up");
    var policy =
        new PolicySnapshot(DEGRADATION_ID, Duration.ofSeconds(30), signalSource, NOW);

    when(policyStoreClient.getAllPolicies()).thenReturn(Map.of(DEGRADATION_ID, policy));

    var holder = new RegisteredDegradation(DEGRADATION_ID);
    holder.replacePolicy(policy); // same policy already set

    when(registry.find(DEGRADATION_ID)).thenReturn(Optional.of(holder));

    policyRefreshService.refreshAllPolicies();

    verify(registry).sync(Set.of(DEGRADATION_ID));
    verify(evaluationScheduler, never()).scheduleNow(anyString());
  }

  @Test
  void refreshAllPolicies_intervalChanged_reschedulesEvaluation() {
    var signalSource =
        new SignalSourceSnapshot(SignalSourceSnapshot.SignalSourceType.PROMETHEUS, "up");
    var oldPolicy =
        new PolicySnapshot(DEGRADATION_ID, Duration.ofSeconds(30), signalSource, NOW);
    var newPolicy =
        new PolicySnapshot(DEGRADATION_ID, Duration.ofSeconds(60), signalSource, NOW);

    when(policyStoreClient.getAllPolicies()).thenReturn(Map.of(DEGRADATION_ID, newPolicy));

    var holder = new RegisteredDegradation(DEGRADATION_ID);
    holder.replacePolicy(oldPolicy);

    when(registry.find(DEGRADATION_ID)).thenReturn(Optional.of(holder));

    policyRefreshService.refreshAllPolicies();

    verify(evaluationScheduler).scheduleNow(DEGRADATION_ID);
  }

  @Test
  void refreshAllPolicies_signalSourceChanged_reschedulesEvaluation() {
    var oldSource =
        new SignalSourceSnapshot(SignalSourceSnapshot.SignalSourceType.PROMETHEUS, "metric_a");
    var newSource =
        new SignalSourceSnapshot(SignalSourceSnapshot.SignalSourceType.PROMETHEUS, "metric_b");
    var oldPolicy =
        new PolicySnapshot(DEGRADATION_ID, Duration.ofSeconds(30), oldSource, NOW);
    var newPolicy =
        new PolicySnapshot(DEGRADATION_ID, Duration.ofSeconds(30), newSource, NOW);

    when(policyStoreClient.getAllPolicies()).thenReturn(Map.of(DEGRADATION_ID, newPolicy));

    var holder = new RegisteredDegradation(DEGRADATION_ID);
    holder.replacePolicy(oldPolicy);

    when(registry.find(DEGRADATION_ID)).thenReturn(Optional.of(holder));

    policyRefreshService.refreshAllPolicies();

    verify(evaluationScheduler).scheduleNow(DEGRADATION_ID);
  }

  @Test
  void refreshAllPolicies_emptyPolicies_syncsEmptySet() {
    when(policyStoreClient.getAllPolicies()).thenReturn(Map.of());

    policyRefreshService.refreshAllPolicies();

    verify(registry).sync(Set.of());
    verifyNoInteractions(evaluationScheduler);
  }

  @Test
  void refreshAllPolicies_inactiveHolder_skipsUpdate() {
    var signalSource =
        new SignalSourceSnapshot(SignalSourceSnapshot.SignalSourceType.PROMETHEUS, "up");
    var policy =
        new PolicySnapshot(DEGRADATION_ID, Duration.ofSeconds(30), signalSource, NOW);

    when(policyStoreClient.getAllPolicies()).thenReturn(Map.of(DEGRADATION_ID, policy));

    var holder = new RegisteredDegradation(DEGRADATION_ID);
    holder.deactivate();

    when(registry.find(DEGRADATION_ID)).thenReturn(Optional.of(holder));

    policyRefreshService.refreshAllPolicies();

    verify(registry).sync(Set.of(DEGRADATION_ID));
    verify(evaluationScheduler, never()).scheduleNow(anyString());
  }

  @Test
  void refreshAllPolicies_holderNotFoundInRegistry_noScheduling() {
    var signalSource =
        new SignalSourceSnapshot(SignalSourceSnapshot.SignalSourceType.PROMETHEUS, "up");
    var policy =
        new PolicySnapshot(DEGRADATION_ID, Duration.ofSeconds(30), signalSource, NOW);

    when(policyStoreClient.getAllPolicies()).thenReturn(Map.of(DEGRADATION_ID, policy));
    when(registry.find(DEGRADATION_ID)).thenReturn(Optional.empty());

    policyRefreshService.refreshAllPolicies();

    verify(registry).sync(Set.of(DEGRADATION_ID));
    verifyNoInteractions(evaluationScheduler);
  }
}
