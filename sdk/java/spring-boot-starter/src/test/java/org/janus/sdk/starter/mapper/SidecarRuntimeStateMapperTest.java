package org.janus.sdk.starter.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.util.Durations;
import java.time.Duration;
import org.janus.api.sidecar.Degradation;
import org.junit.jupiter.api.Test;

class SidecarRuntimeStateMapperTest {

  private final SidecarRuntimeStateMapper mapper = new SidecarRuntimeStateMapper();

  @Test
  void mapsRequiredFieldsCorrectly() {
    var degradation =
        Degradation.newBuilder()
            .setDegradationId("deg-1")
            .setValue(0.75)
            .setEvaluationInterval(Durations.fromMillis(30000))
            .build();

    var state = mapper.toRuntimeState(degradation);

    assertThat(state.degradationId()).isEqualTo("deg-1");
    assertThat(state.value()).isEqualTo(0.75);
    assertThat(state.evaluationInterval()).isEqualTo(Duration.ofMillis(30000));
    assertThat(state.stale()).isFalse();
    assertThat(state.loadedAt()).isNotNull();
  }

  @Test
  void mapsOptionalFieldsToNaNWhenAbsent() {
    var degradation =
        Degradation.newBuilder()
            .setDegradationId("deg-nan")
            .setValue(0.5)
            .setEvaluationInterval(Durations.fromMillis(10000))
            .build();

    var state = mapper.toRuntimeState(degradation);

    assertThat(state.criticalThreshold()).isNaN();
    assertThat(state.minFallbackRatio()).isNaN();
    assertThat(state.maxFallbackRatio()).isNaN();
    assertThat(state.fallbackCurveExponent()).isNaN();
  }

  @Test
  void mapsOptionalFieldsToValuesWhenPresent() {
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
            .build();

    var state = mapper.toRuntimeState(degradation);

    assertThat(state.evaluationInterval()).isEqualTo(Duration.ofMinutes(1));
  }
}
