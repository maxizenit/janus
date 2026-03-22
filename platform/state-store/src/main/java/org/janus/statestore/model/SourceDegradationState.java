package org.janus.statestore.model;

import java.time.Duration;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record SourceDegradationState(
    DegradationStateUpdateSource source, double value, Duration remainingTtl) {}
