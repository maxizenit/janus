package org.janus.evaluator.client.leadership;

import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class RedisLeadershipHandle implements LeadershipHandle {

  private final boolean acquired;
  private final Runnable releaseAction;
  private final AtomicBoolean released = new AtomicBoolean(false);

  public RedisLeadershipHandle(boolean acquired, Runnable releaseAction) {
    this.acquired = acquired;
    this.releaseAction = releaseAction;
  }

  @Override
  public boolean acquired() {
    return acquired;
  }

  @Override
  public void release() {
    if (acquired && released.compareAndSet(false, true)) {
      releaseAction.run();
    }
  }

  @Override
  public void close() {
    // Redis leadership keys are TTL-based; close must not shorten the lease.
  }
}
