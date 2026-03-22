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
    boolean stale,
    Instant loadedAt) {}
