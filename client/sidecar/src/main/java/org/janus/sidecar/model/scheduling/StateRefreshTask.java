package org.janus.sidecar.model.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record StateRefreshTask(String degradationId, Instant scheduledAt) implements Delayed {

  @Override
  public long getDelay(TimeUnit unit) {
    var delayMillis = Duration.between(Instant.now(), scheduledAt).toMillis();
    return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
  }

  @Override
  public int compareTo(Delayed o) {
    if (o instanceof StateRefreshTask other) {
      return this.scheduledAt.compareTo(other.scheduledAt);
    }
    return 0;
  }

  public static StateRefreshTask immediate(String degradationId) {
    return new StateRefreshTask(degradationId, Instant.now());
  }
}
