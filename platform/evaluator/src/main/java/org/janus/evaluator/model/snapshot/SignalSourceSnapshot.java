package org.janus.evaluator.model.snapshot;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record SignalSourceSnapshot(SignalSourceType type, String reference) {
  public enum SignalSourceType {
    DEGRADATION,
    PROMETHEUS
  }
}
