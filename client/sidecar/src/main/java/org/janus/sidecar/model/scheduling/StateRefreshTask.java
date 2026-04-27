package org.janus.sidecar.model.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record StateRefreshTask(String degradationId, Instant scheduledAt, long nanoDeadline)
    implements Delayed {

  public static StateRefreshTask scheduledAt(String degradationId, Instant scheduledAt) {
    var delayNanos = Duration.between(Instant.now(), scheduledAt).toNanos();
    return new StateRefreshTask(degradationId, scheduledAt, System.nanoTime() + delayNanos);
  }

  public static StateRefreshTask immediate(String degradationId) {
    return new StateRefreshTask(degradationId, Instant.EPOCH, System.nanoTime() - 1);
  }

  @Override
  public long getDelay(TimeUnit unit) {
    return unit.convert(nanoDeadline - System.nanoTime(), TimeUnit.NANOSECONDS);
  }

  @Override
  public int compareTo(Delayed o) {
    if (o instanceof StateRefreshTask other) {
      return Long.compare(this.nanoDeadline, other.nanoDeadline);
    }
    return 0;
  }
}
