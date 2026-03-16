package org.janus.sidecar.service;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.janus.sidecar.scheduling.StateRefreshScheduler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@NullMarked
public class InitialLoadService {

  private final StateRefreshScheduler stateRefreshScheduler;

  public void scheduleInitialStateLoad(Set<String> degradationIds) {
    degradationIds.forEach(stateRefreshScheduler::scheduleNow);
  }
}
