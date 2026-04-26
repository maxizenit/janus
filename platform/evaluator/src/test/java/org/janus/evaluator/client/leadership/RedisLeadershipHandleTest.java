package org.janus.evaluator.client.leadership;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RedisLeadershipHandleTest {

  @Test
  void close_doesNotReleaseLease() {
    var releaseCount = new AtomicInteger();
    var handle = new RedisLeadershipHandle(true, releaseCount::incrementAndGet);

    handle.close();

    assertThat(releaseCount).hasValue(0);
  }

  @Test
  void release_acquiredHandleRunsReleaseActionOnce() {
    var releaseCount = new AtomicInteger();
    var handle = new RedisLeadershipHandle(true, releaseCount::incrementAndGet);

    handle.release();
    handle.release();

    assertThat(releaseCount).hasValue(1);
  }

  @Test
  void release_rejectedHandleDoesNotRunReleaseAction() {
    var releaseCount = new AtomicInteger();
    var handle = new RedisLeadershipHandle(false, releaseCount::incrementAndGet);

    handle.release();

    assertThat(releaseCount).hasValue(0);
  }
}
