package org.janus.evaluator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class InitialLoadService {

  private final PolicyRefreshService policyRefreshService;

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    log.info("Initial evaluator policy load started");
    try {
      policyRefreshService.refreshAllPolicies();
      log.info("Initial evaluator policy load completed");
    } catch (RuntimeException exception) {
      log.warn(
          "Initial evaluator policy load failed, scheduled refresh will retry",
          exception);
    }
  }
}
