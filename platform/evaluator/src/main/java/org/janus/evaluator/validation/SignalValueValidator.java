package org.janus.evaluator.validation;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@NullMarked
public class SignalValueValidator {

  public double validate(double value, String degradationId) {
    if (Double.isNaN(value)) {
      log.debug(
          "NaN signal value treated as 0 (no data): degradation={}",
          degradationId);
      return 0.0;
    }

    double clamped = Math.clamp(value, 0.0, 1.0);
    if (clamped != value) {
      log.debug(
          "Signal value clamped into [0.0, 1.0]: degradation={}, raw={}, clamped={}",
          degradationId,
          value,
          clamped);
    }
    return clamped;
  }
}
