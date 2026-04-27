package org.janus.sdk.starter.mapper;

import com.google.protobuf.util.Durations;
import java.time.Duration;
import java.time.Instant;
import org.janus.api.sidecar.Degradation;
import org.janus.sdk.core.runtime.DegradationRuntimeState;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class SidecarRuntimeStateMapper {

  public DegradationRuntimeState toRuntimeState(Degradation degradation) {
    return new DegradationRuntimeState(
        degradation.getDegradationId(),
        degradation.getValue(),
        Duration.ofMillis(Durations.toMillis(degradation.getEvaluationInterval())),
        degradation.getCriticalThreshold(),
        degradation.getMinFallbackRatio(),
        degradation.getMaxFallbackRatio(),
        degradation.getFallbackCurveExponent(),
        degradation.getStale(),
        Instant.now());
  }
}
