package org.janus.sdk.core.fallback;

import java.util.concurrent.ThreadLocalRandom;
import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.janus.sdk.core.runtime.DegradationRuntimeState;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DefaultFallbackDecisionService implements FallbackDecisionService {

  @Override
  public FallbackDecision decide(
      DegradableMethodDescriptor descriptor, @Nullable DegradationRuntimeState runtimeState) {
    if (runtimeState == null) {
      return new FallbackDecision(false, 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0);
    }

    double criticalThreshold =
        !Double.isNaN(descriptor.criticalThreshold())
            ? descriptor.criticalThreshold()
            : runtimeState.criticalThreshold();

    double degradationValue = runtimeState.value();

    double minFallbackRatio =
        !Double.isNaN(descriptor.minFallbackRatio())
            ? descriptor.minFallbackRatio()
            : runtimeState.minFallbackRatio();

    double maxFallbackRatio =
        !Double.isNaN(descriptor.maxFallbackRatio())
            ? descriptor.maxFallbackRatio()
            : runtimeState.maxFallbackRatio();

    double fallbackCurveExponent =
        !Double.isNaN(descriptor.fallbackCurveExponent())
            ? descriptor.fallbackCurveExponent()
            : runtimeState.fallbackCurveExponent();

    double fallbackRatio =
        calculateFallbackRatio(
            degradationValue,
            criticalThreshold,
            minFallbackRatio,
            maxFallbackRatio,
            fallbackCurveExponent);

    boolean fallbackRequired =
        degradationValue >= criticalThreshold
            && ThreadLocalRandom.current().nextDouble() < fallbackRatio;

    return new FallbackDecision(
        fallbackRequired,
        degradationValue,
        criticalThreshold,
        minFallbackRatio,
        maxFallbackRatio,
        fallbackRatio);
  }

  private double calculateFallbackRatio(
      double degradationValue,
      double criticalThreshold,
      double minFallbackRatio,
      double maxFallbackRatio,
      double fallbackCurveExponent) {

    if (degradationValue < criticalThreshold) {
      return 0.0;
    }

    if (criticalThreshold >= 1.0) {
      return maxFallbackRatio;
    }

    double normalized =
        (Math.min(degradationValue, 1.0) - criticalThreshold) / (1.0 - criticalThreshold);

    double curved = Math.pow(normalized, fallbackCurveExponent);

    return minFallbackRatio + curved * (maxFallbackRatio - minFallbackRatio);
  }
}
