package org.janus.adminui.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.janus.adminui.model.PolicyView;
import org.janus.adminui.model.SignalSourceTypeView;
import org.junit.jupiter.api.Test;

class PolicyViewMapperTest {

  private final PolicyViewMapper mapper = new PolicyViewMapper();

  @Test
  void toUpdateRequest_nullFallbackFields_addsPathsWithoutValues() {
    var view =
        new PolicyView(
            "recommendations",
            Duration.ofSeconds(30),
            SignalSourceTypeView.MANUAL,
            null,
            null,
            null,
            null,
            null);

    var request = mapper.toUpdateRequest(view);

    assertThat(request.getUpdateMask().getPathsList())
        .contains(
            "critical_threshold",
            "min_fallback_ratio",
            "max_fallback_ratio",
            "fallback_curve_exponent");
    assertThat(request.hasCriticalThreshold()).isFalse();
    assertThat(request.hasMinFallbackRatio()).isFalse();
    assertThat(request.hasMaxFallbackRatio()).isFalse();
    assertThat(request.hasFallbackCurveExponent()).isFalse();
  }

  @Test
  void toUpdateRequest_nonNullFallbackFields_addsPathsAndValues() {
    var view =
        new PolicyView(
            "recommendations",
            Duration.ofSeconds(30),
            SignalSourceTypeView.MANUAL,
            null,
            0.7,
            0.1,
            0.9,
            2.0);

    var request = mapper.toUpdateRequest(view);

    assertThat(request.getUpdateMask().getPathsList())
        .contains(
            "critical_threshold",
            "min_fallback_ratio",
            "max_fallback_ratio",
            "fallback_curve_exponent");
    assertThat(request.getCriticalThreshold()).isEqualTo(0.7);
    assertThat(request.getMinFallbackRatio()).isEqualTo(0.1);
    assertThat(request.getMaxFallbackRatio()).isEqualTo(0.9);
    assertThat(request.getFallbackCurveExponent()).isEqualTo(2.0);
  }
}
