package org.janus.evaluator.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
  void validate_validValues_returnsValue(double value) {
    assertThat(validator.validate(value, DEGRADATION_ID)).isEqualTo(value);
  }

  @Test
  void validate_nan_throwsException() {
    assertThatThrownBy(() -> validator.validate(Double.NaN, DEGRADATION_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("finite");
  }

  @Test
  void validate_positiveInfinity_throwsException() {
    assertThatThrownBy(() -> validator.validate(Double.POSITIVE_INFINITY, DEGRADATION_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("finite");
  }

  @Test
  void validate_negativeInfinity_throwsException() {
    assertThatThrownBy(() -> validator.validate(Double.NEGATIVE_INFINITY, DEGRADATION_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("finite");
  }

  @Test
  void validate_negativeValue_throwsException() {
    assertThatThrownBy(() -> validator.validate(-0.1, DEGRADATION_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("[0.0, 1.0]");
  }

  @Test
  void validate_greaterThanOne_throwsException() {
    assertThatThrownBy(() -> validator.validate(1.01, DEGRADATION_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("[0.0, 1.0]");
  }

  @Test
  void validate_errorMessageContainsDegradationId() {
    assertThatThrownBy(() -> validator.validate(2.0, DEGRADATION_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(DEGRADATION_ID);
  }
}
