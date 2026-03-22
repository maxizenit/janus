package org.janus.sdk.core.transform;

import static org.janus.sdk.annotation.param.Direction.DECREASE;
import static org.janus.sdk.annotation.param.Direction.INCREASE;

import java.math.BigDecimal;
import org.janus.sdk.annotation.param.Direction;
import org.janus.sdk.core.descriptor.AbsoluteScaleDescriptor;
import org.janus.sdk.core.descriptor.BoundsDescriptor;
import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.janus.sdk.core.descriptor.ParameterDescriptor;
import org.janus.sdk.core.descriptor.RelativeScaleDescriptor;
import org.janus.sdk.core.fallback.FallbackDecision;
import org.janus.sdk.core.validation.InvalidDegradableDefinitionException;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DefaultFallbackArgumentsTransformer implements FallbackArgumentsTransformer {

  @Override
  public Object[] transform(
      DegradableMethodDescriptor descriptor,
      FallbackDecision decision,
      Object[] originalArguments) {
    Object[] transformed = originalArguments.clone();

    for (ParameterDescriptor parameter : descriptor.parameters()) {
      if (!parameter.isScaled()) {
        continue;
      }

      Object originalArgument = transformed[parameter.index()];
      if (originalArgument == null) {
        throw new InvalidDegradableDefinitionException(
            "Scaled parameter must not be null at runtime: method=%s, parameterIndex=%d"
                .formatted(descriptor.method().toGenericString(), parameter.index()));
      }

      double normalizedRatio = decision.normalizedFallbackRatio();

      if (parameter.absoluteScale() != null) {
        transformed[parameter.index()] =
            transformAbsolute(
                parameter.parameterType(), parameter.absoluteScale(), normalizedRatio);
        continue;
      }

      if (parameter.relativeScale() != null) {
        transformed[parameter.index()] =
            transformRelative(
                parameter.parameterType(),
                originalArgument,
                parameter.relativeScale(),
                parameter.bounds(),
                normalizedRatio);
      }
    }

    return transformed;
  }

  private Object transformAbsolute(
      Class<?> targetType, AbsoluteScaleDescriptor scale, double normalizedRatio) {
    double scaled = interpolate(scale.min(), scale.max(), normalizedRatio, scale.direction());

    return cast(targetType, scaled);
  }

  private Object transformRelative(
      Class<?> targetType,
      Object originalArgument,
      RelativeScaleDescriptor scale,
      BoundsDescriptor bounds,
      double normalizedRatio) {
    double originalValue = asDouble(originalArgument);
    double factor =
        interpolate(scale.minFactor(), scale.maxFactor(), normalizedRatio, scale.direction());

    double scaled = originalValue * factor;

    if (bounds != null) {
      scaled = Math.max(bounds.min(), Math.min(bounds.max(), scaled));
    }

    return cast(targetType, scaled);
  }

  private double interpolate(double min, double max, double normalizedRatio, Direction direction) {
    return switch (direction) {
      case DECREASE -> max - normalizedRatio * (max - min);
      case INCREASE -> min + normalizedRatio * (max - min);
    };
  }

  private double asDouble(Object value) {
    if (value instanceof BigDecimal bigDecimal) {
      return bigDecimal.doubleValue();
    }
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    throw new InvalidDegradableDefinitionException(
        "Unsupported numeric argument type: " + value.getClass().getName());
  }

  private Object cast(Class<?> targetType, double value) {
    if (targetType == byte.class || targetType == Byte.class) {
      return (byte) Math.round(value);
    }
    if (targetType == short.class || targetType == Short.class) {
      return (short) Math.round(value);
    }
    if (targetType == int.class || targetType == Integer.class) {
      return (int) Math.round(value);
    }
    if (targetType == long.class || targetType == Long.class) {
      return Math.round(value);
    }
    if (targetType == float.class || targetType == Float.class) {
      return (float) value;
    }
    if (targetType == double.class || targetType == Double.class) {
      return value;
    }
    if (targetType == BigDecimal.class) {
      return BigDecimal.valueOf(value);
    }

    throw new InvalidDegradableDefinitionException(
        "Unsupported target numeric type: " + targetType.getName());
  }
}
