package org.janus.sidecar.configuration.properties;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import java.time.Duration;
import org.janus.sidecar.configuration.properties.SidecarProperties.DefaultThresholds;
import org.junit.jupiter.api.Test;

class SidecarPropertiesTest {

  @Test
  void validate_defaultThresholdsWithMinGreaterThanMax_hasViolation() {
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      var validator = validatorFactory.getValidator();
      var properties =
          new SidecarProperties(
              Duration.ofSeconds(30),
              4,
              1000,
              1000,
              "./janus-sidecar.db",
              new DefaultThresholds(0.5, 0.9, 0.2, 2.0));

      var violations = validator.validate(properties);

      assertThat(violations)
          .anySatisfy(
              violation ->
                  assertThat(violation.getMessage())
                      .isEqualTo(
                          "minFallbackRatio must be less than or equal to maxFallbackRatio"));
    }
  }

  @Test
  void validate_defaultThresholdsWithOrderedRatios_hasNoViolations() {
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      var validator = validatorFactory.getValidator();
      var properties =
          new SidecarProperties(
              Duration.ofSeconds(30),
              4,
              1000,
              1000,
              "./janus-sidecar.db",
              new DefaultThresholds(0.5, 0.2, 0.9, 2.0));

      assertThat(validator.validate(properties)).isEmpty();
    }
  }
}
