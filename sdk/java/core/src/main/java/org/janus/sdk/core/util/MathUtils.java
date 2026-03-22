package org.janus.sdk.core.util;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class MathUtils {

  private MathUtils() {}

  public static double normalizeClamped(double value, double min, double max) {
    if (max <= min) {
      return value >= max ? 1.0 : 0.0;
    }

    double clamped = Math.max(min, Math.min(max, value));
    return (clamped - min) / (max - min);
  }
}
