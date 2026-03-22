package org.janus.sdk.core.fallback;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record FallbackDecision(
    boolean fallbackRequired,
    double degradationValue,
    double effectiveCriticalThreshold,
    double effectiveMinFallbackRatio,
    double effectiveMaxFallbackRatio,
    double normalizedFallbackRatio) {}
