package org.janus.evaluator.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SignalValueValidatorTest {

  private static final String DEGRADATION_ID = "test-degradation";

  private SignalValueValidator validator;

  @BeforeEach
  void setUp() {
    validator = new SignalValueValidator();
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, 0.001, 0.5, 0.999, 1.0})
  void validate_inRangeValue_returnedAsIs(double value) {
    assertThat(validator.validate(value, DEGRADATION_ID)).isEqualTo(value);
  }

  @Test
  void validate_nan_treatedAsZero() {
    assertThat(validator.validate(Double.NaN, DEGRADATION_ID)).isEqualTo(0.0);
  }

  @Test
  void validate_positiveInfinity_clampedToOne() {
    assertThat(validator.validate(Double.POSITIVE_INFINITY, DEGRADATION_ID)).isEqualTo(1.0);
  }

  @Test
  void validate_negativeInfinity_clampedToZero() {
    assertThat(validator.validate(Double.NEGATIVE_INFINITY, DEGRADATION_ID)).isEqualTo(0.0);
  }

  @Test
  void validate_negativeValue_clampedToZero() {
    assertThat(validator.validate(-0.1, DEGRADATION_ID)).isEqualTo(0.0);
  }

  @Test
  void validate_greaterThanOne_clampedToOne() {
    assertThat(validator.validate(1.01, DEGRADATION_ID)).isEqualTo(1.0);
  }

  @Test
  void validate_largeValue_clampedToOne() {
    assertThat(validator.validate(42.0, DEGRADATION_ID)).isEqualTo(1.0);
  }
}
