package org.janus.sidecar.configuration.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("janus.sidecar")
@Validated
public record SidecarProperties(
    @NotNull @DurationMin(millis = 1) Duration policyRefreshInterval,
    @Positive int stateRefreshThreads,
    @Positive int stateRefreshQueueCapacity,
    @Positive int maxSyncDegradationIds,
    @NotNull String sqlitePath,
    @Valid @Nullable DefaultThresholds defaultThresholds) {

  public record DefaultThresholds(
      @DecimalMin("0.0") @DecimalMax("1.0") @Nullable Double criticalThreshold,
      @DecimalMin("0.0") @DecimalMax("1.0") @Nullable Double minFallbackRatio,
      @DecimalMin("0.0") @DecimalMax("1.0") @Nullable Double maxFallbackRatio,
      @DecimalMin(value = "0.0", inclusive = false) @Nullable Double fallbackCurveExponent) {}
}
