package org.janus.statestore.model;

import java.time.Duration;

public record DegradationStateUpdate(
        String degradationId,
        double value,
        Duration ttl,
        DegradationStateUpdateSource source
) {
}
