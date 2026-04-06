package org.janus.sdk.core.fallback;

import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.janus.sdk.core.runtime.DegradationRuntimeState;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@RequiredArgsConstructor
public class DefaultFallbackDecisionService implements FallbackDecisionService {

  private final StaleDegradationStrategy staleStrategy;

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

    double degradationValue = resolveValue(runtimeState);

    double minFallbackRatio =
        !Double.isNaN(descriptor.minFallbackRatio())
            ? descriptor.minFallbackRatio()
            : runtimeState.minFallbackRatio();

    double maxFallbackRatio =
        !Double.isNaN(descriptor.maxFallbackRatio())
            ? descriptor.maxFallbackRatio()
            : runtimeState.maxFallbackRatio();

    double fallbackRatio =
        calculateFallbackRatio(
            degradationValue, criticalThreshold, minFallbackRatio, maxFallbackRatio);

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
      double maxFallbackRatio) {

    if (degradationValue < criticalThreshold) {
      return 0.0;
    }

    if (criticalThreshold >= 1.0) {
      return maxFallbackRatio;
    }

    double normalized =
        (Math.min(degradationValue, 1.0) - criticalThreshold) / (1.0 - criticalThreshold);

    return minFallbackRatio + normalized * (maxFallbackRatio - minFallbackRatio);
  }

  private double resolveValue(DegradationRuntimeState runtimeState) {
    if (!runtimeState.stale()) {
      return runtimeState.value();
    }

    return switch (staleStrategy) {
      case LAST_VALUE -> runtimeState.value();
      case FAIL_SAFE -> 1.0;
      case FAIL_OPEN -> 0.0;
    };
  }
}
