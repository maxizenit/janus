package org.janus.decider.client.leadership;

import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class RedisLeadershipHandle implements LeadershipHandle {

  private final boolean acquired;
  private final Runnable releaseAction;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  public RedisLeadershipHandle(boolean acquired, Runnable releaseAction) {
    this.acquired = acquired;
    this.releaseAction = releaseAction;
  }

  @Override
  public boolean acquired() {
    return acquired;
  }

  @Override
  public void close() {
    if (acquired && closed.compareAndSet(false, true)) {
      releaseAction.run();
    }
  }
}
