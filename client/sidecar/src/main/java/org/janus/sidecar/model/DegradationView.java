package org.janus.sidecar.model;

import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record DegradationView(
    String degradationId,
    double value,
    Duration evaluationInterval,
    double criticalThreshold,
    double minFallbackRatio,
    double maxFallbackRatio,
    double fallbackCurveExponent,
    Instant stateLoadedAt,
    Instant policyLoadedAt,
    boolean stale) {}
