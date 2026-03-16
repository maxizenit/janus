package org.janus.sidecar.model;

import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record DegradationView(
    String degradationId,
    double value,
    Duration evaluationInterval,
    @Nullable Double criticalThreshold,
    @Nullable Double minFallbackRatio,
    @Nullable Double maxFallbackRatio,
    Instant stateLoadedAt,
    Instant policyLoadedAt,
    boolean stale) {}
