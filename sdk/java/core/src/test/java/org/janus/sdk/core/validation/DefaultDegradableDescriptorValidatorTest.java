package org.janus.sdk.core.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.List;
import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.jspecify.annotations.Nullable;
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
      String degradationId, Method method, @Nullable Method fallbackMethod) {
    return new DegradableMethodDescriptor(
        degradationId,
        method,
        fallbackMethod,
        SampleService.class,
        List.of(),
        List.of());
  }

  @Test
  void validDescriptorWithoutFallback_passes() {
    DegradableMethodDescriptor desc =
        descriptor("my-degradation", method("doWork", int.class), null);

    assertThatCode(() -> validator.validate(desc)).doesNotThrowAnyException();
  }

  @Test
  void validDescriptor_passes() {
    DegradableMethodDescriptor desc =
        descriptor(
            "my-degradation",
            method("doWork", int.class),
            method("doWorkFallback", int.class));

    assertThatCode(() -> validator.validate(desc)).doesNotThrowAnyException();
  }

  @Test
  void blankDegradationId_fails() {
    DegradableMethodDescriptor desc =
        descriptor("  ", method("doWork", int.class), method("doWorkFallback", int.class));

    assertThatThrownBy(() -> validator.validate(desc))
        .isInstanceOf(InvalidDegradableDefinitionException.class)
        .hasMessageContaining("Degradation id must not be blank");
  }

  @Test
  void sameMethodAsFallback_fails() {
    Method m = method("doWork", int.class);
    DegradableMethodDescriptor desc = descriptor("test", m, m);

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
            method("doWorkDifferentParams", int.class, String.class));

    assertThatThrownBy(() -> validator.validate(desc))
        .isInstanceOf(InvalidDegradableDefinitionException.class)
        .hasMessageContaining("same number of parameters");
  }
}
