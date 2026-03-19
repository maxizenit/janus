package org.janus.decider.service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@NullMarked
public class InitialLoadService {

  private final PolicyRefreshService policyRefreshService;

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    policyRefreshService.refreshAllPolicies();
  }
}
