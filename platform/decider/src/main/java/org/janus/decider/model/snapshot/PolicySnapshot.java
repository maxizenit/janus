package org.janus.decider.model.snapshot;

import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record PolicySnapshot(
    String degradationId,
    Duration evaluationInterval,
    SignalSourceSnapshot signalSource,
    Instant loadedAt) {}
