package org.janus.decider.model.scheduling;

import java.time.Instant;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record EvaluationTask(String degradationId, Instant scheduledAt) implements Delayed {

  @Override
  public long getDelay(TimeUnit unit) {
    var delayMillis = scheduledAt.toEpochMilli() - Instant.now().toEpochMilli();
    return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
  }

  @Override
  public int compareTo(Delayed o) {
    if (o instanceof EvaluationTask other) {
      return this.scheduledAt.compareTo(other.scheduledAt);
    }
    return 0;
  }

  public static EvaluationTask immediate(String degradationId) {
    return new EvaluationTask(degradationId, Instant.EPOCH);
  }
}
