package org.janus.evaluator.client.leadership;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class RedisLeadershipHandle implements LeadershipHandle {

  private final boolean acquired;

  public RedisLeadershipHandle(boolean acquired) {
    this.acquired = acquired;
  }

  @Override
  public boolean acquired() {
    return acquired;
  }

  @Override
  public void close() {
    // Redis leadership keys are TTL-based; close must not shorten the lease.
  }
}
