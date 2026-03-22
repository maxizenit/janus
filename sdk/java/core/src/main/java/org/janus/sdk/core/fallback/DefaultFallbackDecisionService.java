package org.janus.sdk.core.fallback;

import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.janus.sdk.core.runtime.DegradationRuntimeState;
import org.janus.sdk.core.util.MathUtils;
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

    double minFallbackRatio =
        !Double.isNaN(descriptor.minFallbackRatio())
            ? descriptor.minFallbackRatio()
            : runtimeState.minFallbackRatio();

    double maxFallbackRatio =
        !Double.isNaN(descriptor.maxFallbackRatio())
            ? descriptor.maxFallbackRatio()
            : runtimeState.maxFallbackRatio();

    double degradationValue = runtimeState.value();
    boolean fallbackRequired = degradationValue >= criticalThreshold;

    double normalizedFallbackRatio =
        MathUtils.normalizeClamped(degradationValue, minFallbackRatio, maxFallbackRatio);

    return new FallbackDecision(
        fallbackRequired,
        degradationValue,
        criticalThreshold,
        minFallbackRatio,
        maxFallbackRatio,
        normalizedFallbackRatio);
  }
}
