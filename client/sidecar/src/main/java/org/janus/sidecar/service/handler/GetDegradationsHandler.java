package org.janus.sidecar.service.handler;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.janus.sidecar.configuration.properties.SidecarProperties;
import org.janus.sidecar.configuration.properties.SidecarProperties.DefaultThresholds;
import org.janus.sidecar.model.DegradationView;
import org.janus.sidecar.model.RegisteredDegradation;
import org.janus.sidecar.registry.ActualDegradationRegistry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@NullMarked
public class GetDegradationsHandler {

  private final ActualDegradationRegistry registry;
  private final SidecarProperties properties;

  public List<DegradationView> handle() {
    return registry.findAllActive().stream()
        .map(this::toViewOrNull)
        .filter(Objects::nonNull)
        .toList();
  }

  private @Nullable DegradationView toViewOrNull(RegisteredDegradation holder) {
    var policy = holder.getPolicy().orElse(null);
    var state = holder.getState().orElse(null);

    if (policy == null || state == null) {
      return null;
    }

    var fallbackRatios =
        resolveFallbackRatios(policy.minFallbackRatio(), policy.maxFallbackRatio());

    return new DegradationView(
        holder.getDegradationId(),
        state.value(),
        policy.evaluationInterval(),
        resolveWithDefault(policy.criticalThreshold(), DefaultThresholds::criticalThreshold),
        fallbackRatios.minFallbackRatio(),
        fallbackRatios.maxFallbackRatio(),
        resolveWithDefault(
            policy.fallbackCurveExponent(), DefaultThresholds::fallbackCurveExponent),
        state.loadedAt(),
        policy.loadedAt(),
        state.stale());
  }

  private @Nullable Double resolveWithDefault(
      @Nullable Double policyValue,
      Function<DefaultThresholds, @Nullable Double> defaultExtractor) {
    if (policyValue != null) {
      return policyValue;
    }
    var defaults = properties.defaultThresholds();
    return defaults != null ? defaultExtractor.apply(defaults) : null;
  }

  private ResolvedFallbackRatios resolveFallbackRatios(
      @Nullable Double policyMinFallbackRatio, @Nullable Double policyMaxFallbackRatio) {
    if (policyMinFallbackRatio != null || policyMaxFallbackRatio != null) {
      return new ResolvedFallbackRatios(policyMinFallbackRatio, policyMaxFallbackRatio);
    }

    var defaults = properties.defaultThresholds();
    return defaults == null
        ? new ResolvedFallbackRatios(null, null)
        : new ResolvedFallbackRatios(
            defaults.minFallbackRatio(), defaults.maxFallbackRatio());
  }

  private record ResolvedFallbackRatios(
      @Nullable Double minFallbackRatio, @Nullable Double maxFallbackRatio) {}
}
