package org.janus.adminui.model;

import java.time.Duration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record PolicyView(
    String degradationId,
    Duration evaluationInterval,
    SignalSourceTypeView signalSourceType,
    @Nullable String query,
    double criticalThreshold,
    double minFallbackRatio,
    double maxFallbackRatio,
    double fallbackCurveExponent) {}
