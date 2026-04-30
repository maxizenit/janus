package org.janus.sidecar.service;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.sidecar.scheduling.StateRefreshScheduler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class InitialLoadService {

  private final StateRefreshScheduler stateRefreshScheduler;

  public void scheduleInitialStateLoad(Set<String> degradationIds) {
    log.info("Scheduling initial state load: count={}", degradationIds.size());
    degradationIds.forEach(stateRefreshScheduler::scheduleNow);
  }
}
