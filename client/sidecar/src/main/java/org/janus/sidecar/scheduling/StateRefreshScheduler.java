package org.janus.sidecar.scheduling;

import java.time.Instant;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface StateRefreshScheduler {

  void scheduleNow(String degradationId);

  void scheduleAt(String degradationId, Instant instant);
}
