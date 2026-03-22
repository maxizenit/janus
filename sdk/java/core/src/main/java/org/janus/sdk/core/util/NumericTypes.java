package org.janus.sdk.core.util;

import java.math.BigDecimal;
import java.util.Set;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class NumericTypes {

  private static final Set<Class<?>> SUPPORTED_TYPES =
      Set.of(
          byte.class,
          Byte.class,
          short.class,
          Short.class,
          int.class,
          Integer.class,
          long.class,
          Long.class,
          float.class,
          Float.class,
          double.class,
          Double.class,
          BigDecimal.class);

  private NumericTypes() {}

  public static boolean isSupported(Class<?> type) {
    return SUPPORTED_TYPES.contains(type);
  }
}
