package org.janus.statestore.model;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record DegradationState(String degradationId, double value) {}
