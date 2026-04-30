package org.janus.sdk.starter.mapper;

import com.google.protobuf.util.Durations;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.janus.api.sidecar.Degradation;
import org.janus.sdk.core.runtime.DegradationRuntimeState;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@NullMarked
public class SidecarRuntimeStateMapper {

  private final Clock clock;

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
        Instant.now(clock));
  }
}
