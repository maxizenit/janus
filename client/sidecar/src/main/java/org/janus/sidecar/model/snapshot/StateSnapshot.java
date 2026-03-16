package org.janus.sidecar.model.snapshot;

import java.time.Instant;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record StateSnapshot(String degradationId, double value, Instant loadedAt, boolean stale) {
  public StateSnapshot staleCopy(Instant loadedAt) {
    return new StateSnapshot(degradationId, value, loadedAt, true);
  }
}
