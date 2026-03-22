package org.janus.statestore.model;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record EffectiveDegradationState(double value, DegradationStateUpdateSource source) {}
