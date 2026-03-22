package org.janus.sdk.core.descriptor;

import org.janus.sdk.annotation.param.Direction;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record AbsoluteScaleDescriptor(double min, double max, Direction direction) {}
