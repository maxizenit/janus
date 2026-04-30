package org.janus.sidecar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.janus.sidecar.client.policystore.PolicyStoreClient;
import org.janus.sidecar.model.RegisteredDegradation;
import org.janus.sidecar.model.snapshot.PolicySnapshot;
import org.janus.sidecar.model.snapshot.StateSnapshot;
import org.janus.sidecar.registry.ActualDegradationRegistry;
import org.janus.sidecar.scheduling.StateRefreshScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyRefreshServiceTest {

  private static final Instant NOW = Instant.parse("2026-04-24T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Mock private ActualDegradationRegistry registry;
  @Mock private PolicyStoreClient policyStoreClient;
  @Mock private StateRefreshScheduler stateRefreshScheduler;

  @Test
  void refreshPolicies_missingPolicyClearsCachedPolicyAndState() {
    var service = new PolicyRefreshService(registry, policyStoreClient, stateRefreshScheduler, CLOCK);
    var holder = new RegisteredDegradation("deg-1");
    holder.replacePolicy(
        new PolicySnapshot("deg-1", Duration.ofSeconds(10), 0.9, 0.1, 0.8, 2.0, NOW));
    holder.setState(new StateSnapshot("deg-1", 0.5, NOW, false));

    when(policyStoreClient.getPolicies(Set.of("deg-1"))).thenReturn(Map.of());
    when(registry.find("deg-1")).thenReturn(Optional.of(holder));

    var refreshed = service.refreshPolicies(Set.of("deg-1"));

    assertThat(refreshed).isEmpty();
    assertThat(holder.getPolicy()).isEmpty();
    assertThat(holder.getState()).isEmpty();
    verifyNoInteractions(stateRefreshScheduler);
  }
}
