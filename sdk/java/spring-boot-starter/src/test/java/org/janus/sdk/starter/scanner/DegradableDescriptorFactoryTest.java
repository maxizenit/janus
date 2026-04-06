package org.janus.sdk.starter.scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.janus.sdk.annotation.Degradable;
import org.janus.sdk.annotation.param.AbsoluteScale;
import org.janus.sdk.annotation.param.Direction;
import org.janus.sdk.annotation.param.RelativeScale;
import org.janus.sdk.core.validation.InvalidDegradableDefinitionException;
import org.junit.jupiter.api.Test;

class DegradableDescriptorFactoryTest {

  private final DegradableDescriptorFactory factory = new DegradableDescriptorFactory();

  // --- helper classes ---

  @SuppressWarnings("unused")
  static class ValidService {

    @Degradable(
        value = "test-degradation",
        fallback = "fallbackMethod",
        criticalThreshold = 0.8,
        minFallbackRatio = 0.1,
        maxFallbackRatio = 0.9,
        fallbackCurveExponent = 2.0)
    public String primaryMethod(String input) {
      return input;
    }

    public String fallbackMethod(String input) {
      return "fallback:" + input;
    }
  }

  @SuppressWarnings("unused")
  static class DefaultsService {

    @Degradable(value = "defaults-degradation", fallback = "fallbackDefaults")
    public void primaryDefaults() {}

    public void fallbackDefaults() {}
  }

  @SuppressWarnings("unused")
  static class MissingFallbackService {

    @Degradable(value = "missing-fb", fallback = "nonExistentMethod")
    public void primary() {}
  }

  @SuppressWarnings("unused")
  static class BlankFallbackService {

    @Degradable(value = "blank-fb", fallback = "  ")
    public void primary() {}
  }

  @SuppressWarnings("unused")
  static class EmptyFallbackService {

    @Degradable(value = "empty-fb")
    public void primary() {}
  }

  @SuppressWarnings("unused")
  static class ScaledParamsService {

    @Degradable(value = "scaled", fallback = "scaledFallback")
    public void scaledMethod(
        @AbsoluteScale(min = 10.0, max = 100.0, direction = Direction.INCREASE) int count,
        @RelativeScale(
                minFactor = 0.2,
                maxFactor = 0.8,
                direction = Direction.DECREASE,
                min = 5.0,
                max = 50.0)
            double factor,
        String plain) {}

    public void scaledFallback(int count, double factor, String plain) {}
  }

  // --- tests ---

  @Test
  void createsDescriptorWithAllAnnotationValues() throws Exception {
    var method = ValidService.class.getDeclaredMethod("primaryMethod", String.class);

    var descriptor = factory.create(ValidService.class, method);

    assertThat(descriptor.degradationId()).isEqualTo("test-degradation");
    assertThat(descriptor.method()).isEqualTo(method);
    assertThat(descriptor.fallbackMethod().getName()).isEqualTo("fallbackMethod");
    assertThat(descriptor.beanClass()).isEqualTo(ValidService.class);
    assertThat(descriptor.criticalThreshold()).isEqualTo(0.8);
    assertThat(descriptor.minFallbackRatio()).isEqualTo(0.1);
    assertThat(descriptor.maxFallbackRatio()).isEqualTo(0.9);
    assertThat(descriptor.fallbackCurveExponent()).isEqualTo(2.0);
  }

  @Test
  void preservesNaNDefaultsWhenNotSpecified() throws Exception {
    var method = DefaultsService.class.getDeclaredMethod("primaryDefaults");

    var descriptor = factory.create(DefaultsService.class, method);

    assertThat(descriptor.degradationId()).isEqualTo("defaults-degradation");
    assertThat(descriptor.criticalThreshold()).isNaN();
    assertThat(descriptor.minFallbackRatio()).isNaN();
    assertThat(descriptor.maxFallbackRatio()).isNaN();
    assertThat(descriptor.fallbackCurveExponent()).isNaN();
  }

  @Test
  void throwsWhenFallbackMethodNotFound() throws Exception {
    var method = MissingFallbackService.class.getDeclaredMethod("primary");

    assertThatThrownBy(() -> factory.create(MissingFallbackService.class, method))
        .isInstanceOf(InvalidDegradableDefinitionException.class)
        .hasMessageContaining("Fallback method not found");
  }

  @Test
  void throwsWhenFallbackNameIsBlank() throws Exception {
    var method = BlankFallbackService.class.getDeclaredMethod("primary");

    assertThatThrownBy(() -> factory.create(BlankFallbackService.class, method))
        .isInstanceOf(InvalidDegradableDefinitionException.class)
        .hasMessageContaining("must not be blank");
  }

  @Test
  void throwsWhenFallbackNameIsEmpty() throws Exception {
    var method = EmptyFallbackService.class.getDeclaredMethod("primary");

    assertThatThrownBy(() -> factory.create(EmptyFallbackService.class, method))
        .isInstanceOf(InvalidDegradableDefinitionException.class)
        .hasMessageContaining("must not be blank");
  }

  @Test
  void createsParameterDescriptorsForAbsoluteScale() throws Exception {
    var method =
        ScaledParamsService.class.getDeclaredMethod(
            "scaledMethod", int.class, double.class, String.class);

    var descriptor = factory.create(ScaledParamsService.class, method);
    var parameters = descriptor.parameters();

    assertThat(parameters).hasSize(3);

    var absoluteParam = parameters.get(0);
    assertThat(absoluteParam.index()).isEqualTo(0);
    assertThat(absoluteParam.parameterType()).isEqualTo(int.class);
    assertThat(absoluteParam.hasAbsoluteScale()).isTrue();
    assertThat(absoluteParam.hasRelativeScale()).isFalse();
    assertThat(absoluteParam.absoluteScale().min()).isEqualTo(10.0);
    assertThat(absoluteParam.absoluteScale().max()).isEqualTo(100.0);
    assertThat(absoluteParam.absoluteScale().direction()).isEqualTo(Direction.INCREASE);
  }

  @Test
  void createsParameterDescriptorsForRelativeScale() throws Exception {
    var method =
        ScaledParamsService.class.getDeclaredMethod(
            "scaledMethod", int.class, double.class, String.class);

    var descriptor = factory.create(ScaledParamsService.class, method);
    var parameters = descriptor.parameters();

    var relativeParam = parameters.get(1);
    assertThat(relativeParam.index()).isEqualTo(1);
    assertThat(relativeParam.parameterType()).isEqualTo(double.class);
    assertThat(relativeParam.hasRelativeScale()).isTrue();
    assertThat(relativeParam.hasAbsoluteScale()).isFalse();
    assertThat(relativeParam.relativeScale().minFactor()).isEqualTo(0.2);
    assertThat(relativeParam.relativeScale().maxFactor()).isEqualTo(0.8);
    assertThat(relativeParam.relativeScale().direction()).isEqualTo(Direction.DECREASE);
    assertThat(relativeParam.relativeScale().min()).isEqualTo(5.0);
    assertThat(relativeParam.relativeScale().max()).isEqualTo(50.0);
  }

  @Test
  void createsParameterDescriptorWithoutScaleForPlainParam() throws Exception {
    var method =
        ScaledParamsService.class.getDeclaredMethod(
            "scaledMethod", int.class, double.class, String.class);

    var descriptor = factory.create(ScaledParamsService.class, method);
    var plainParam = descriptor.parameters().get(2);

    assertThat(plainParam.index()).isEqualTo(2);
    assertThat(plainParam.parameterType()).isEqualTo(String.class);
    assertThat(plainParam.hasAbsoluteScale()).isFalse();
    assertThat(plainParam.hasRelativeScale()).isFalse();
    assertThat(plainParam.isScaled()).isFalse();
  }
}
