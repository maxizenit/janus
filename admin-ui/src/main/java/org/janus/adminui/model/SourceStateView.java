package org.janus.adminui.model;

import java.time.Duration;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record SourceStateView(String source, double value, Duration remainingTtl) {}
