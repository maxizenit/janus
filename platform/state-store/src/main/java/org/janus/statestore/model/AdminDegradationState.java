package org.janus.statestore.model;

import java.util.List;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record AdminDegradationState(
    String degradationId,
    EffectiveDegradationState effectiveState,
    List<SourceDegradationState> sourceStates) {}
