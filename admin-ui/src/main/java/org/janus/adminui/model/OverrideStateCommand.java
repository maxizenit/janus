package org.janus.adminui.model;

import java.time.Duration;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record OverrideStateCommand(String degradationId, double value, Duration ttl) {}
