package org.janus.sdk.core.descriptor;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record BoundsDescriptor(double min, double max) {}
