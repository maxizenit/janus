package org.janus.sdk.core.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.List;
import org.janus.sdk.core.descriptor.DegradableMethodDescriptor;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class FallbackCycleDetectorTest {

  @SuppressWarnings("unused")
  static class SampleService {
    public String a(int x) {
      return "";
    }

    public String b(int x) {
      return "";
    }

    public String c(int x) {
      return "";
    }

    public String d(int x) {
      return "";
    }

    public String plainLeaf(int x) {
      return "";
    }
  }

  private final FallbackCycleDetector detector = new FallbackCycleDetector();

  private Method method(String name) {
    try {
      return SampleService.class.getDeclaredMethod(name, int.class);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  private DegradableMethodDescriptor descriptor(String id, Method method, @Nullable Method fallback) {
    return new DegradableMethodDescriptor(
        id, method, fallback, SampleService.class, List.of(), List.of());
  }

  @Test
  void leafFallbackNotDegradable_passes() {
    var descA = descriptor("a", method("a"), method("plainLeaf"));

    assertThatCode(() -> detector.detect(List.of(descA))).doesNotThrowAnyException();
  }

  @Test
  void directCycleAtoB_fails() {
    var descA = descriptor("a", method("a"), method("b"));
    var descB = descriptor("b", method("b"), method("a"));

    assertThatThrownBy(() -> detector.detect(List.of(descA, descB)))
        .isInstanceOf(InvalidDegradableDefinitionException.class)
        .hasMessageContaining("cycle")
        .hasMessageContaining("a(int)")
        .hasMessageContaining("b(int)");
  }

  @Test
  void transitiveCycleAtoBtoCtoA_fails() {
    var descA = descriptor("a", method("a"), method("b"));
    var descB = descriptor("b", method("b"), method("c"));
    var descC = descriptor("c", method("c"), method("a"));

    assertThatThrownBy(() -> detector.detect(List.of(descA, descB, descC)))
        .isInstanceOf(InvalidDegradableDefinitionException.class)
        .hasMessageContaining("cycle")
        .hasMessageContaining("a(int)")
        .hasMessageContaining("b(int)")
        .hasMessageContaining("c(int)");
  }

  @Test
  void twoIndependentChainsWithoutCycles_passes() {
    var descA = descriptor("a", method("a"), method("b"));
    var descB = descriptor("b", method("b"), method("plainLeaf"));
    var descC = descriptor("c", method("c"), method("d"));
    var descD = descriptor("d", method("d"), null);

    assertThatCode(() -> detector.detect(List.of(descA, descB, descC, descD)))
        .doesNotThrowAnyException();
  }

  @Test
  void singleMethodWithoutFallback_passes() {
    var descA = descriptor("a", method("a"), null);

    assertThatCode(() -> detector.detect(List.of(descA))).doesNotThrowAnyException();
  }

  @Test
  void emptyRegistry_passes() {
    assertThatCode(() -> detector.detect(List.of())).doesNotThrowAnyException();
  }
}
