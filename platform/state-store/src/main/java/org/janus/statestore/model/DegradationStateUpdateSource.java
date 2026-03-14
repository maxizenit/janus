package org.janus.statestore.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DegradationStateUpdateSource {
  ADMIN_UI(0),
  DECIDER(1);

  private final int priority;
}
