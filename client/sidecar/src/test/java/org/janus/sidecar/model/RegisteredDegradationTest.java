package org.janus.sidecar.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.janus.sidecar.model.snapshot.PolicySnapshot;
import org.janus.sidecar.model.snapshot.StateSnapshot;
import org.junit.jupiter.api.Test;

class RegisteredDegradationTest {

  @Test
  void getPolicy_initiallyEmpty() {
    var degradation = new RegisteredDegradation("deg-1");
    assertThat(degradation.getPolicy()).isEmpty();
  }

  @Test
  void getState_initiallyEmpty() {
    var degradation = new RegisteredDegradation("deg-1");
    assertThat(degradation.getState()).isEmpty();
  }

  @Test
  void replacePolicy_updatesValue() {
    var degradation = new RegisteredDegradation("deg-1");
    var snapshot =
        new PolicySnapshot("deg-1", Duration.ofSeconds(5), 0.9, 0.1, 0.8, 2.0, Instant.now());

    degradation.replacePolicy(snapshot);

    assertThat(degradation.getPolicy()).hasValue(snapshot);
  }

  @Test
  void replacePolicy_replacesExistingValue() {
    var degradation = new RegisteredDegradation("deg-1");
    var first =
        new PolicySnapshot("deg-1", Duration.ofSeconds(5), 0.9, null, null, null, Instant.now());
    var second =
        new PolicySnapshot("deg-1", Duration.ofSeconds(10), 0.7, 0.2, 0.6, 3.0, Instant.now());

    degradation.replacePolicy(first);
    degradation.replacePolicy(second);

    assertThat(degradation.getPolicy()).hasValue(second);
  }

  @Test
  void clearPolicy_removesExistingValue() {
    var degradation = new RegisteredDegradation("deg-1");
    var snapshot =
        new PolicySnapshot("deg-1", Duration.ofSeconds(5), 0.9, null, null, null, Instant.now());

    degradation.replacePolicy(snapshot);
    degradation.clearPolicy();

    assertThat(degradation.getPolicy()).isEmpty();
  }

  @Test
  void setState_updatesValue() {
    var degradation = new RegisteredDegradation("deg-1");
    var snapshot = new StateSnapshot("deg-1", 0.5, Instant.now(), false);

    degradation.setState(snapshot);

    assertThat(degradation.getState()).hasValue(snapshot);
  }

  @Test
  void clearState_removesExistingValue() {
    var degradation = new RegisteredDegradation("deg-1");
    var snapshot = new StateSnapshot("deg-1", 0.5, Instant.now(), false);

    degradation.setState(snapshot);
    degradation.clearState();

    assertThat(degradation.getState()).isEmpty();
  }

  @Test
  void markStateStale_marksExistingState() {
    var degradation = new RegisteredDegradation("deg-1");
    var loadedAt = Instant.parse("2026-04-24T10:00:00Z");
    var staleLoadedAt = Instant.parse("2026-04-24T10:01:00Z");
    degradation.setState(new StateSnapshot("deg-1", 0.5, loadedAt, false));

    boolean marked = degradation.markStateStale(staleLoadedAt);

    assertThat(marked).isTrue();
    assertThat(degradation.getState())
        .hasValue(new StateSnapshot("deg-1", 0.5, staleLoadedAt, true));
  }

  @Test
  void markStateStale_withoutState_returnsFalse() {
    var degradation = new RegisteredDegradation("deg-1");

    boolean marked = degradation.markStateStale(Instant.now());

    assertThat(marked).isFalse();
  }

  @Test
  void isActive_trueByDefault() {
    var degradation = new RegisteredDegradation("deg-1");
    assertThat(degradation.isActive()).isTrue();
  }

  @Test
  void deactivate_makesInactive() {
    var degradation = new RegisteredDegradation("deg-1");

    degradation.deactivate();

    assertThat(degradation.isActive()).isFalse();
  }

  @Test
  void deactivate_idempotent() {
    var degradation = new RegisteredDegradation("deg-1");

    degradation.deactivate();
    degradation.deactivate();

    assertThat(degradation.isActive()).isFalse();
  }
}
