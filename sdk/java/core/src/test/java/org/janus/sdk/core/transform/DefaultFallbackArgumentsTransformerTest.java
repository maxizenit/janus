package org.janus.sdk.core.transform;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import org.janus.sdk.annotation.param.Direction;
import org.janus.sdk.core.descriptor.AbsoluteScaleDescriptor;
import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.janus.sdk.core.descriptor.ParameterDescriptor;
import org.janus.sdk.core.descriptor.RelativeScaleDescriptor;
import org.janus.sdk.core.fallback.FallbackDecision;
import org.junit.jupiter.api.Test;

class DefaultFallbackArgumentsTransformerTest {

  @SuppressWarnings("unused")
  static class SampleService {
    public String doWork(int limit) {
      return "";
    }

    public String doWorkFallback(int limit) {
      return "fallback";
    }

    public String doByte(byte limit) {
      return "";
    }

    public String doByteFallback(byte limit) {
      return "fallback";
    }

    public String doShort(short limit) {
      return "";
    }

    public String doShortFallback(short limit) {
      return "fallback";
    }
  }

  private final DefaultFallbackArgumentsTransformer transformer =
      new DefaultFallbackArgumentsTransformer();

  private Method method(String name, Class<?>... paramTypes) {
    try {
      return SampleService.class.getDeclaredMethod(name, paramTypes);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  private DegradableMethodDescriptor descriptor(List<ParameterDescriptor> parameters) {
    return new DegradableMethodDescriptor(
        "test-degradation",
        method("doWork", int.class),
        method("doWorkFallback", int.class),
        SampleService.class,
        parameters,
        List.of());
  }

  private DegradableMethodDescriptor descriptor(
      String methodName, String fallbackName, Class<?> paramType, ParameterDescriptor parameter) {
    return new DegradableMethodDescriptor(
        "test-degradation",
        method(methodName, paramType),
        method(fallbackName, paramType),
        SampleService.class,
        List.of(parameter),
        List.of());
  }

  private FallbackDecision decision(double fallbackRatio) {
    return new FallbackDecision(true, 0.8, 0.5, 0.0, 1.0, fallbackRatio);
  }

  @Test
  void noScale_argsUnchanged() {
    ParameterDescriptor param = new ParameterDescriptor(0, int.class, null, null);
    DegradableMethodDescriptor desc = descriptor(List.of(param));
    Object[] args = new Object[] {100};

    Object[] result = transformer.transform(desc, decision(0.5), args);

    assertThat(result[0]).isEqualTo(100);
  }

  @Test
  void absoluteScale_decrease() {
    // DECREASE: max - ratio * (max - min) = 100 - 0.5 * (100 - 10) = 100 - 45 = 55
    AbsoluteScaleDescriptor absScale = new AbsoluteScaleDescriptor(10.0, 100.0, Direction.DECREASE);
    ParameterDescriptor param = new ParameterDescriptor(0, int.class, absScale, null);
    DegradableMethodDescriptor desc = descriptor(List.of(param));
    Object[] args = new Object[] {80};

    Object[] result = transformer.transform(desc, decision(0.5), args);

    // cast to int: Math.round(55.0) = 55
    assertThat(result[0]).isEqualTo(55);
  }

  @Test
  void absoluteScale_byteTarget_clampsAboveMaxValue() {
    AbsoluteScaleDescriptor absScale =
        new AbsoluteScaleDescriptor(10.0, 1000.0, Direction.INCREASE);
    ParameterDescriptor param = new ParameterDescriptor(0, byte.class, absScale, null);
    DegradableMethodDescriptor desc = descriptor("doByte", "doByteFallback", byte.class, param);
    Object[] args = new Object[] {(byte) 0};

    Object[] result = transformer.transform(desc, decision(1.0), args);

    assertThat(result[0]).isEqualTo(Byte.MAX_VALUE);
  }

  @Test
  void absoluteScale_shortTarget_clampsBelowMinValue() {
    AbsoluteScaleDescriptor absScale =
        new AbsoluteScaleDescriptor(-100_000.0, 100.0, Direction.DECREASE);
    ParameterDescriptor param = new ParameterDescriptor(0, short.class, absScale, null);
    DegradableMethodDescriptor desc = descriptor("doShort", "doShortFallback", short.class, param);
    Object[] args = new Object[] {(short) 0};

    Object[] result = transformer.transform(desc, decision(1.0), args);

    assertThat(result[0]).isEqualTo(Short.MIN_VALUE);
  }

  @Test
  void relativeScale_intTarget_clampsAboveMaxValue() {
    RelativeScaleDescriptor relScale =
        new RelativeScaleDescriptor(1.0, 10.0, Direction.INCREASE, Double.NaN, Double.NaN);
    ParameterDescriptor param = new ParameterDescriptor(0, int.class, null, relScale);
    DegradableMethodDescriptor desc = descriptor(List.of(param));
    Object[] args = new Object[] {Integer.MAX_VALUE};

    Object[] result = transformer.transform(desc, decision(1.0), args);

    assertThat(result[0]).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  void relativeScale_increase() {
    // INCREASE: min + ratio * (max - min) = 1.0 + 0.5 * (2.0 - 1.0) = 1.5
    // scaled = originalValue * factor = 100 * 1.5 = 150
    RelativeScaleDescriptor relScale =
        new RelativeScaleDescriptor(1.0, 2.0, Direction.INCREASE, Double.NaN, Double.NaN);
    ParameterDescriptor param = new ParameterDescriptor(0, int.class, null, relScale);
    DegradableMethodDescriptor desc = descriptor(List.of(param));
    Object[] args = new Object[] {100};

    Object[] result = transformer.transform(desc, decision(0.5), args);

    // cast to int: Math.round(150.0) = 150
    assertThat(result[0]).isEqualTo(150);
  }
}
