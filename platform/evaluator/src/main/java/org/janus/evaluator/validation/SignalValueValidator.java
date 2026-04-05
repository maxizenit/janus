package org.janus.evaluator.validation;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class SignalValueValidator {

  public double validate(double value, String degradationId) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      throw new IllegalArgumentException(
          "Signal value must be finite: degradation=" + degradationId);
    }

    if (value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(
          "Signal value must be in range [0.0, 1.0]: degradation="
              + degradationId
              + ", value="
              + value);
    }

    return value;
  }
}
