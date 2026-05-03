package org.janus.adminui.ui.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.janus.adminui.ui.dialog.StateOverrideDialog.TtlUnit;
import org.junit.jupiter.api.Test;

class StateOverrideDialogTest {

  @Test
  void validate_emptyValue_returnsError() {
    var result =
        StateOverrideDialog.validate("deg-1", null, 5, TtlUnit.MINUTES, Duration.ofHours(1));

    assertThat(result.command()).isNull();
    assertThat(result.errorMessage()).isEqualTo("Value must be in range [0.0, 1.0]");
  }

  @Test
  void validate_outOfRangeValue_returnsError() {
    var result =
        StateOverrideDialog.validate("deg-1", 1.5, 5, TtlUnit.MINUTES, Duration.ofHours(1));

    assertThat(result.errorMessage()).isEqualTo("Value must be in range [0.0, 1.0]");
  }

  @Test
  void validate_emptyTtl_returnsError() {
    var result =
        StateOverrideDialog.validate("deg-1", 0.5, null, TtlUnit.MINUTES, Duration.ofHours(1));

    assertThat(result.errorMessage()).isEqualTo("TTL must be positive");
  }

  @Test
  void validate_zeroTtl_returnsError() {
    var result =
        StateOverrideDialog.validate("deg-1", 0.5, 0, TtlUnit.MINUTES, Duration.ofHours(1));

    assertThat(result.errorMessage()).isEqualTo("TTL must be positive");
  }

  @Test
  void validate_nullUnit_returnsError() {
    var result = StateOverrideDialog.validate("deg-1", 0.5, 5, null, Duration.ofHours(1));

    assertThat(result.errorMessage()).isEqualTo("TTL unit is required");
  }

  @Test
  void validate_ttlAboveMax_returnsError() {
    var result =
        StateOverrideDialog.validate("deg-1", 0.5, 6, TtlUnit.MINUTES, Duration.ofMinutes(5));

    assertThat(result.errorMessage()).isEqualTo("TTL must not exceed 5 minutes");
  }

  @Test
  void validate_subMinuteMaxTtl_returnsCommandWithSeconds() {
    var result =
        StateOverrideDialog.validate("deg-1", 0.7, 30, TtlUnit.SECONDS, Duration.ofSeconds(30));

    assertThat(result.errorMessage()).isNull();
    assertThat(result.command()).isNotNull();
    assertThat(result.command().degradationId()).isEqualTo("deg-1");
    assertThat(result.command().value()).isEqualTo(0.7);
    assertThat(result.command().ttl()).isEqualTo(Duration.ofSeconds(30));
  }
}
