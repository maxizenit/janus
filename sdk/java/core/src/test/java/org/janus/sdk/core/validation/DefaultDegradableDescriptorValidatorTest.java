package org.janus.sdk.core.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.List;
import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.junit.jupiter.api.Test;

class DefaultDegradableDescriptorValidatorTest {

  @SuppressWarnings("unused")
  static class SampleService {
    public String doWork(int limit) {
      return "";
    }

    public String doWorkFallback(int limit) {
      return "fallback";
    }

    public String doWorkDifferentParams(int limit, String extra) {
      return "";
    }

    public void voidMethod(int limit) {}
  }

  private final DefaultDegradableDescriptorValidator validator =
      new DefaultDegradableDescriptorValidator();

  private Method method(String name, Class<?>... paramTypes) {
    try {
      return SampleService.class.getDeclaredMethod(name, paramTypes);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  private DegradableMethodDescriptor descriptor(
      String degradationId,
      Method method,
      Method fallbackMethod,
      double criticalThreshold,
      double minRatio,
      double maxRatio) {
    return new DegradableMethodDescriptor(
        degradationId,
        method,
        fallbackMethod,
        SampleService.class,
        criticalThreshold,
        minRatio,
        maxRatio,
        Double.NaN,
        List.of());
  }

  @Test
  void validDescriptor_passes() {
    DegradableMethodDescriptor desc =
        descriptor(
            "my-degradation",
            method("doWork", int.class),
            method("doWorkFallback", int.class),
            0.5,
            0.1,
            0.9);

    assertThatCode(() -> validator.validate(desc)).doesNotThrowAnyException();
  }

  @Test
  void blankDegradationId_fails() {
    DegradableMethodDescriptor desc =
        descriptor(
            "  ",
            method("doWork", int.class),
            method("doWorkFallback", int.class),
            0.5,
            0.1,
            0.9);

    assertThatThrownBy(() -> validator.validate(desc))
        .isInstanceOf(InvalidDegradableDefinitionException.class)
        .hasMessageContaining("Degradation id must not be blank");
  }

  @Test
  void sameMethodAsFallback_fails() {
    Method m = method("doWork", int.class);
    DegradableMethodDescriptor desc = descriptor("test", m, m, 0.5, 0.1, 0.9);

    assertThatThrownBy(() -> validator.validate(desc))
        .isInstanceOf(InvalidDegradableDefinitionException.class)
        .hasMessageContaining("Fallback method must differ");
  }

  @Test
  void parameterCountMismatch_fails() {
    DegradableMethodDescriptor desc =
        descriptor(
            "test",
            method("doWork", int.class),
            method("doWorkDifferentParams", int.class, String.class),
            0.5,
            0.1,
            0.9);

    assertThatThrownBy(() -> validator.validate(desc))
        .isInstanceOf(InvalidDegradableDefinitionException.class)
        .hasMessageContaining("same number of parameters");
  }

  @Test
  void criticalThresholdOutOfRange_fails() {
    DegradableMethodDescriptor desc =
        descriptor(
            "test",
            method("doWork", int.class),
            method("doWorkFallback", int.class),
            1.5,
            0.1,
            0.9);

    assertThatThrownBy(() -> validator.validate(desc))
        .isInstanceOf(InvalidDegradableDefinitionException.class)
        .hasMessageContaining("criticalThreshold must be in range");
  }

  @Test
  void minFallbackRatioGreaterThanMax_fails() {
    DegradableMethodDescriptor desc =
        descriptor(
            "test",
            method("doWork", int.class),
            method("doWorkFallback", int.class),
            0.5,
            0.9,
            0.1);

    assertThatThrownBy(() -> validator.validate(desc))
        .isInstanceOf(InvalidDegradableDefinitionException.class)
        .hasMessageContaining("minFallbackRatio must be less than or equal to maxFallbackRatio");
  }

  @Test
  void nanValues_pass() {
    DegradableMethodDescriptor desc =
        descriptor(
            "test",
            method("doWork", int.class),
            method("doWorkFallback", int.class),
            Double.NaN,
            Double.NaN,
            Double.NaN);

    assertThatCode(() -> validator.validate(desc)).doesNotThrowAnyException();
  }
}
