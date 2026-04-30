package org.janus.sdk.core.runtime;

import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record DegradationRuntimeState(
    String degradationId,
    double value,
    Duration evaluationInterval,
    double criticalThreshold,
    double minFallbackRatio,
    double maxFallbackRatio,
    double fallbackCurveExponent,
    boolean stale,
    Instant loadedAt) {

  public DegradationRuntimeState staleCopy() {
    return new DegradationRuntimeState(
        degradationId,
        value,
        evaluationInterval,
        criticalThreshold,
        minFallbackRatio,
        maxFallbackRatio,
        fallbackCurveExponent,
        true,
        loadedAt);
  }
}
