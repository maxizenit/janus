package org.janus.adminui.model;

import java.time.Duration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record PolicyView(
    String degradationId,
    Duration evaluationInterval,
    SignalSourceTypeView signalSourceType,
    @Nullable String sourceDegradationId,
    @Nullable String metricReference,
    @Nullable Double criticalThreshold,
    @Nullable Double minFallbackRatio,
    @Nullable Double maxFallbackRatio) {}
