package org.janus.statestore.model;

import java.time.Duration;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record DegradationStateUpdate(
    String degradationId, double value, DegradationStateUpdateSource source, Duration ttl) {}
