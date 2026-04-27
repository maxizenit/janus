package org.janus.evaluator.model.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class EvaluationTaskTest {

  @Test
  void scheduledAtInTheFutureProducesPositiveDelay() {
    var task = EvaluationTask.scheduledAt("deg", Instant.now().plus(Duration.ofSeconds(5)));

    assertThat(task.getDelay(TimeUnit.MILLISECONDS)).isPositive();
  }

  @Test
  void scheduledAtInThePastProducesNonPositiveDelay() {
    var task = EvaluationTask.scheduledAt("deg", Instant.now().minus(Duration.ofSeconds(5)));

    assertThat(task.getDelay(TimeUnit.MILLISECONDS)).isNotPositive();
  }

  @Test
  void immediateIsReadyAheadOfFutureTasks() {
    var future = EvaluationTask.scheduledAt("deg", Instant.now().plus(Duration.ofSeconds(1)));
    var immediate = EvaluationTask.immediate("deg");

    assertThat(immediate.compareTo(future)).isNegative();
    assertThat(immediate.getDelay(TimeUnit.MILLISECONDS)).isNotPositive();
  }

  @Test
  void compareToOrdersByDeadline() {
    var earlier = EvaluationTask.scheduledAt("a", Instant.now().plus(Duration.ofMillis(100)));
    var later = EvaluationTask.scheduledAt("b", Instant.now().plus(Duration.ofSeconds(10)));

    assertThat(earlier.compareTo(later)).isNegative();
    assertThat(later.compareTo(earlier)).isPositive();
  }

  @Test
  void scheduledAtPreservesScheduledInstantForObservability() {
    var instant = Instant.parse("2026-01-01T00:00:00Z");
    var task = EvaluationTask.scheduledAt("deg", instant);

    assertThat(task.scheduledAt()).isEqualTo(instant);
    assertThat(task.degradationId()).isEqualTo("deg");
  }
}
