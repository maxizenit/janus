package org.janus.statestore.model;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DegradationStateUpdateSource {
  ADMIN_UI,
  EVALUATOR;

  public static List<DegradationStateUpdateSource> resolutionOrder() {
    return List.of(ADMIN_UI, EVALUATOR);
  }
}
