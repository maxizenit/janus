package org.janus.sidecar.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.util.Durations;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.janus.api.sidecar.Degradation;
import org.janus.api.sidecar.GetDegradationsResponse;
import org.janus.sidecar.model.DegradationView;
import org.junit.jupiter.api.Test;

class SidecarGrpcMapperTest {

  private final SidecarGrpcMapper mapper = new SidecarGrpcMapper();

  @Test
  void fromDegradationViewsToGetDegradationsResponse_allFieldsPresent() {
    var view =
        new DegradationView(
            "deg-1",
            0.75,
            Duration.ofSeconds(10),
            0.9,
            0.1,
            0.8,
            2.0,
            Instant.now(),
            Instant.now(),
            false);

    GetDegradationsResponse response =
        mapper.fromDegradationViewsToGetDegradationsResponse(List.of(view));

    assertThat(response.getDegradationsCount()).isEqualTo(1);
    Degradation d = response.getDegradations(0);
    assertThat(d.getDegradationId()).isEqualTo("deg-1");
    assertThat(d.getValue()).isEqualTo(0.75);
    assertThat(Durations.toMillis(d.getEvaluationInterval())).isEqualTo(10_000);
    assertThat(d.getCriticalThreshold()).isEqualTo(0.9);
    assertThat(d.getMinFallbackRatio()).isEqualTo(0.1);
    assertThat(d.getMaxFallbackRatio()).isEqualTo(0.8);
    assertThat(d.getFallbackCurveExponent()).isEqualTo(2.0);
    assertThat(d.getStale()).isFalse();
  }

  @Test
  void fromDegradationViewsToGetDegradationsResponse_requiredFields_setInProto() {
    var view =
        new DegradationView(
            "deg-3",
            0.3,
            Duration.ofSeconds(5),
            0.5,
            0.2,
            0.7,
            3.0,
            Instant.now(),
            Instant.now(),
            true);

    GetDegradationsResponse response =
        mapper.fromDegradationViewsToGetDegradationsResponse(List.of(view));

    Degradation d = response.getDegradations(0);
    assertThat(d.getCriticalThreshold()).isEqualTo(0.5);
    assertThat(d.getMinFallbackRatio()).isEqualTo(0.2);
    assertThat(d.getMaxFallbackRatio()).isEqualTo(0.7);
    assertThat(d.getFallbackCurveExponent()).isEqualTo(3.0);
    assertThat(d.getStale()).isTrue();
  }

  @Test
  void fromDegradationViewsToGetDegradationsResponse_emptyList_emptyResponse() {
    GetDegradationsResponse response =
        mapper.fromDegradationViewsToGetDegradationsResponse(List.of());

    assertThat(response.getDegradationsCount()).isZero();
  }
}
