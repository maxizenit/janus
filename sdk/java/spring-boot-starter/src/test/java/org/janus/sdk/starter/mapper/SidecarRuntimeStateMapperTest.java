package org.janus.sdk.starter.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.util.Durations;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.janus.api.sidecar.Degradation;
import org.junit.jupiter.api.Test;

class SidecarRuntimeStateMapperTest {

  private static final Instant NOW = Instant.parse("2026-04-24T10:00:00Z");
  private final SidecarRuntimeStateMapper mapper =
      new SidecarRuntimeStateMapper(Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void mapsRequiredFieldsCorrectly() {
    var degradation =
        Degradation.newBuilder()
            .setDegradationId("deg-1")
            .setValue(0.75)
            .setEvaluationInterval(Durations.fromMillis(30000))
            .setCriticalThreshold(0.9)
            .setMinFallbackRatio(0.1)
            .setMaxFallbackRatio(0.8)
            .setFallbackCurveExponent(2.5)
            .build();

    var state = mapper.toRuntimeState(degradation);

    assertThat(state.degradationId()).isEqualTo("deg-1");
    assertThat(state.value()).isEqualTo(0.75);
    assertThat(state.evaluationInterval()).isEqualTo(Duration.ofMillis(30000));
    assertThat(state.criticalThreshold()).isEqualTo(0.9);
    assertThat(state.minFallbackRatio()).isEqualTo(0.1);
    assertThat(state.maxFallbackRatio()).isEqualTo(0.8);
    assertThat(state.fallbackCurveExponent()).isEqualTo(2.5);
    assertThat(state.stale()).isFalse();
    assertThat(state.loadedAt()).isEqualTo(NOW);
  }

  @Test
  void mapsPolicyParametersToValues() {
    var degradation =
        Degradation.newBuilder()
            .setDegradationId("deg-full")
            .setValue(0.6)
            .setEvaluationInterval(Durations.fromMillis(20000))
            .setCriticalThreshold(0.9)
            .setMinFallbackRatio(0.1)
            .setMaxFallbackRatio(0.8)
            .setFallbackCurveExponent(2.5)
            .build();

    var state = mapper.toRuntimeState(degradation);

    assertThat(state.criticalThreshold()).isEqualTo(0.9);
    assertThat(state.minFallbackRatio()).isEqualTo(0.1);
    assertThat(state.maxFallbackRatio()).isEqualTo(0.8);
    assertThat(state.fallbackCurveExponent()).isEqualTo(2.5);
  }

  @Test
  void mapsEvaluationIntervalCorrectly() {
    var degradation =
        Degradation.newBuilder()
            .setDegradationId("deg-interval")
            .setValue(0.3)
            .setEvaluationInterval(Durations.fromMillis(60000))
            .setCriticalThreshold(0.9)
            .setMinFallbackRatio(0.1)
            .setMaxFallbackRatio(0.8)
            .setFallbackCurveExponent(2.5)
            .build();

    var state = mapper.toRuntimeState(degradation);

    assertThat(state.evaluationInterval()).isEqualTo(Duration.ofMinutes(1));
  }

  @Test
  void mapsStaleFlagCorrectly() {
    var degradation =
        Degradation.newBuilder()
            .setDegradationId("deg-stale")
            .setValue(0.3)
            .setEvaluationInterval(Durations.fromMillis(60000))
            .setCriticalThreshold(0.9)
            .setMinFallbackRatio(0.1)
            .setMaxFallbackRatio(0.8)
            .setFallbackCurveExponent(2.5)
            .setStale(true)
            .build();

    var state = mapper.toRuntimeState(degradation);

    assertThat(state.stale()).isTrue();
  }
}
