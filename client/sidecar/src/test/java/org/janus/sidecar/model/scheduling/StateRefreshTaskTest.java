package org.janus.sidecar.model.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class StateRefreshTaskTest {

  @Test
  void scheduledAtInTheFutureProducesPositiveDelay() {
    var task = StateRefreshTask.scheduledAt("deg", Instant.now().plus(Duration.ofSeconds(5)));

    assertThat(task.getDelay(TimeUnit.MILLISECONDS)).isPositive();
  }

  @Test
  void scheduledAtInThePastProducesNonPositiveDelay() {
    var task = StateRefreshTask.scheduledAt("deg", Instant.now().minus(Duration.ofSeconds(5)));

    assertThat(task.getDelay(TimeUnit.MILLISECONDS)).isNotPositive();
  }

  @Test
  void immediateIsReadyAheadOfFutureTasks() {
    var future = StateRefreshTask.scheduledAt("deg", Instant.now().plus(Duration.ofSeconds(1)));
    var immediate = StateRefreshTask.immediate("deg");

    assertThat(immediate.compareTo(future)).isNegative();
    assertThat(immediate.getDelay(TimeUnit.MILLISECONDS)).isNotPositive();
  }

  @Test
  void compareToOrdersByDeadline() {
    var earlier = StateRefreshTask.scheduledAt("a", Instant.now().plus(Duration.ofMillis(100)));
    var later = StateRefreshTask.scheduledAt("b", Instant.now().plus(Duration.ofSeconds(10)));

    assertThat(earlier.compareTo(later)).isNegative();
    assertThat(later.compareTo(earlier)).isPositive();
  }

  @Test
  void scheduledAtPreservesScheduledInstantForObservability() {
    var instant = Instant.parse("2026-01-01T00:00:00Z");
    var task = StateRefreshTask.scheduledAt("deg", instant);

    assertThat(task.scheduledAt()).isEqualTo(instant);
    assertThat(task.degradationId()).isEqualTo("deg");
  }
}
