package org.janus.sidecar.model.snapshot;

import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record PolicySnapshot(
    String degradationId,
    Duration evaluationInterval,
    @Nullable Double criticalThreshold,
    @Nullable Double minFallbackRatio,
    @Nullable Double maxFallbackRatio,
    Instant loadedAt) {}
