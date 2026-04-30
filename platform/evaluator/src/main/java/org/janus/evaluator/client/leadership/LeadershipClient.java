package org.janus.evaluator.client.leadership;

import java.time.Duration;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface LeadershipClient {

  LeadershipHandle tryAcquire(String degradationId, Duration leaseDuration);
}
