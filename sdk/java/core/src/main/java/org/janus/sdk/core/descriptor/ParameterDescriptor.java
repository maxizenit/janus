package org.janus.sdk.core.descriptor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record ParameterDescriptor(
    int index,
    Class<?> parameterType,
    @Nullable AbsoluteScaleDescriptor absoluteScale,
    @Nullable RelativeScaleDescriptor relativeScale,
    @Nullable BoundsDescriptor bounds) {

  public boolean hasAbsoluteScale() {
    return absoluteScale != null;
  }

  public boolean hasRelativeScale() {
    return relativeScale != null;
  }

  public boolean hasBounds() {
    return bounds != null;
  }

  public boolean isScaled() {
    return hasAbsoluteScale() || hasRelativeScale();
  }
}
