package org.janus.evaluator.model.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record EvaluationTask(String degradationId, Instant scheduledAt, long nanoDeadline)
    implements Delayed {

  public static EvaluationTask scheduledAt(String degradationId, Instant scheduledAt) {
    var delayNanos = Duration.between(Instant.now(), scheduledAt).toNanos();
    return new EvaluationTask(degradationId, scheduledAt, System.nanoTime() + delayNanos);
  }

  public static EvaluationTask immediate(String degradationId) {
    return new EvaluationTask(degradationId, Instant.EPOCH, System.nanoTime() - 1);
  }

  @Override
  public long getDelay(TimeUnit unit) {
    return unit.convert(nanoDeadline - System.nanoTime(), TimeUnit.NANOSECONDS);
  }

  @Override
  public int compareTo(Delayed o) {
    if (o instanceof EvaluationTask other) {
      return Long.compare(this.nanoDeadline, other.nanoDeadline);
    }
    return 0;
  }
}
