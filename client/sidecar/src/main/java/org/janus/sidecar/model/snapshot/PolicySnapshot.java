package org.janus.sidecar.model.snapshot;

import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record PolicySnapshot(
    String degradationId,
    Duration evaluationInterval,
    double criticalThreshold,
    double minFallbackRatio,
    double maxFallbackRatio,
    double fallbackCurveExponent,
    Instant loadedAt) {}
