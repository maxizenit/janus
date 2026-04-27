package org.janus.sidecar.service.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.janus.sidecar.model.DegradationView;
import org.janus.sidecar.model.RegisteredDegradation;
import org.janus.sidecar.model.snapshot.PolicySnapshot;
import org.janus.sidecar.model.snapshot.StateSnapshot;
import org.janus.sidecar.registry.ActualDegradationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetDegradationsHandlerTest {

  @Mock private ActualDegradationRegistry registry;

  @InjectMocks private GetDegradationsHandler handler;

  @Test
  void handle_activeDegradationsWithPolicyAndState_areReturned() {
    var degradation = new RegisteredDegradation("deg-1");
    var now = Instant.now();
    degradation.replacePolicy(
        new PolicySnapshot("deg-1", Duration.ofSeconds(10), 0.9, 0.1, 0.8, 2.0, now));
    degradation.setState(new StateSnapshot("deg-1", 0.75, now, false));

    when(registry.findAllActive()).thenReturn(List.of(degradation));

    List<DegradationView> result = handler.handle();

    assertThat(result).hasSize(1);
    DegradationView view = result.get(0);
    assertThat(view.degradationId()).isEqualTo("deg-1");
    assertThat(view.value()).isEqualTo(0.75);
    assertThat(view.evaluationInterval()).isEqualTo(Duration.ofSeconds(10));
    assertThat(view.criticalThreshold()).isEqualTo(0.9);
    assertThat(view.minFallbackRatio()).isEqualTo(0.1);
    assertThat(view.maxFallbackRatio()).isEqualTo(0.8);
    assertThat(view.fallbackCurveExponent()).isEqualTo(2.0);
    assertThat(view.stale()).isFalse();
  }

  @Test
  void handle_staleState_propagatesStaleFlag() {
    var degradation = new RegisteredDegradation("deg-1");
    var now = Instant.now();
    degradation.replacePolicy(
        new PolicySnapshot("deg-1", Duration.ofSeconds(10), 0.9, 0.1, 0.8, 2.0, now));
    degradation.setState(new StateSnapshot("deg-1", 0.75, now, true));

    when(registry.findAllActive()).thenReturn(List.of(degradation));

    List<DegradationView> result = handler.handle();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).stale()).isTrue();
  }

  @Test
  void handle_degradationsMissingPolicy_areFilteredOut() {
    var degradation = new RegisteredDegradation("deg-1");
    degradation.setState(new StateSnapshot("deg-1", 0.5, Instant.now(), false));

    when(registry.findAllActive()).thenReturn(List.of(degradation));

    List<DegradationView> result = handler.handle();

    assertThat(result).isEmpty();
  }

  @Test
  void handle_degradationsMissingState_areFilteredOut() {
    var degradation = new RegisteredDegradation("deg-1");
    degradation.replacePolicy(
        new PolicySnapshot("deg-1", Duration.ofSeconds(5), 0.9, 0.1, 0.8, 2.0, Instant.now()));

    when(registry.findAllActive()).thenReturn(List.of(degradation));

    List<DegradationView> result = handler.handle();

    assertThat(result).isEmpty();
  }

  @Test
  void handle_policyValuesAreReturnedWithoutSidecarDefaults() {
    var degradation = new RegisteredDegradation("deg-1");
    var now = Instant.now();
    degradation.replacePolicy(
        new PolicySnapshot("deg-1", Duration.ofSeconds(10), 0.7, 0.2, 0.6, 3.0, now));
    degradation.setState(new StateSnapshot("deg-1", 0.5, now, false));

    when(registry.findAllActive()).thenReturn(List.of(degradation));

    List<DegradationView> result = handler.handle();

    assertThat(result).hasSize(1);
    DegradationView view = result.get(0);
    assertThat(view.criticalThreshold()).isEqualTo(0.7);
    assertThat(view.minFallbackRatio()).isEqualTo(0.2);
    assertThat(view.maxFallbackRatio()).isEqualTo(0.6);
    assertThat(view.fallbackCurveExponent()).isEqualTo(3.0);
  }

}
