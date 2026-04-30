package org.janus.sidecar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.janus.sidecar.client.statestore.StateStoreClient;
import org.janus.sidecar.configuration.properties.SidecarProperties;
import org.janus.sidecar.model.RegisteredDegradation;
import org.janus.sidecar.model.snapshot.PolicySnapshot;
import org.janus.sidecar.model.snapshot.StateSnapshot;
import org.janus.sidecar.registry.ActualDegradationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StateRefreshServiceTest {

  private static final Instant NOW = Instant.parse("2026-04-24T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final Duration POLICY_REFRESH_INTERVAL = Duration.ofSeconds(30);
  private static final SidecarProperties PROPERTIES =
      new SidecarProperties(POLICY_REFRESH_INTERVAL, 1, 1, 1, "");

  @Mock private ActualDegradationRegistry registry;
  @Mock private StateStoreClient stateStoreClient;

  @Test
  void refresh_missingState_marksPreviousStateStaleAndReschedules() {
    var service = new StateRefreshService(registry, stateStoreClient, PROPERTIES, CLOCK);
    var holder = activeHolderWithPolicyAndState();

    when(registry.find("deg-1")).thenReturn(Optional.of(holder));
    when(stateStoreClient.getStates(Set.of("deg-1"))).thenReturn(Map.of());

    var results = service.refresh(Set.of("deg-1"));

    assertThat(results).hasSize(1);
    assertThat(results.get(0).degradationId()).isEqualTo("deg-1");
    assertThat(results.get(0).nextRefreshAt()).isEqualTo(NOW.plusSeconds(10));
    assertThat(holder.getState())
        .hasValue(new StateSnapshot("deg-1", 0.5, NOW.minusSeconds(60), true));
  }

  @Test
  void refresh_stateStoreFailure_marksPreviousStateStaleAndReschedules() {
    var service = new StateRefreshService(registry, stateStoreClient, PROPERTIES, CLOCK);
    var holder = activeHolderWithPolicyAndState();

    when(registry.find("deg-1")).thenReturn(Optional.of(holder));
    when(stateStoreClient.getStates(Set.of("deg-1"))).thenThrow(new RuntimeException("boom"));

    var results = service.refresh(Set.of("deg-1"));

    assertThat(results).hasSize(1);
    assertThat(results.get(0).nextRefreshAt()).isEqualTo(NOW.plusSeconds(10));
    assertThat(holder.getState())
        .hasValue(new StateSnapshot("deg-1", 0.5, NOW.minusSeconds(60), true));
  }

  @Test
  void refresh_withoutPolicy_reschedulesUsingPolicyRefreshInterval() {
    var service = new StateRefreshService(registry, stateStoreClient, PROPERTIES, CLOCK);
    var holder = new RegisteredDegradation("deg-1");

    when(registry.find("deg-1")).thenReturn(Optional.of(holder));
    when(stateStoreClient.getStates(Set.of("deg-1"))).thenReturn(Map.of());

    var results = service.refresh(Set.of("deg-1"));

    assertThat(results).hasSize(1);
    assertThat(results.get(0).nextRefreshAt()).isEqualTo(NOW.plus(POLICY_REFRESH_INTERVAL));
  }

  @Test
  void refresh_withoutPolicy_failurePathStillReschedules() {
    var service = new StateRefreshService(registry, stateStoreClient, PROPERTIES, CLOCK);
    var holder = new RegisteredDegradation("deg-1");

    when(registry.find("deg-1")).thenReturn(Optional.of(holder));
    when(stateStoreClient.getStates(Set.of("deg-1"))).thenThrow(new RuntimeException("boom"));

    var results = service.refresh(Set.of("deg-1"));

    assertThat(results).hasSize(1);
    assertThat(results.get(0).nextRefreshAt()).isEqualTo(NOW.plus(POLICY_REFRESH_INTERVAL));
  }

  private static RegisteredDegradation activeHolderWithPolicyAndState() {
    var holder = new RegisteredDegradation("deg-1");
    holder.replacePolicy(
        new PolicySnapshot("deg-1", Duration.ofSeconds(10), 0.9, 0.1, 0.8, 2.0, NOW));
    holder.setState(new StateSnapshot("deg-1", 0.5, NOW.minusSeconds(60), false));
    return holder;
  }
}
