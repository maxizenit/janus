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

    return new DegradationView(
        holder.getDegradationId(),
        state.value(),
        policy.evaluationInterval(),
        resolveWithDefault(policy.criticalThreshold(), DefaultThresholds::criticalThreshold),
        resolveWithDefault(policy.minFallbackRatio(), DefaultThresholds::minFallbackRatio),
        resolveWithDefault(policy.maxFallbackRatio(), DefaultThresholds::maxFallbackRatio),
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
}
