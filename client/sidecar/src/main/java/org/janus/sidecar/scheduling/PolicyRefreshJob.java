package org.janus.sidecar.scheduling;

import lombok.RequiredArgsConstructor;
import org.janus.sidecar.service.PolicyRefreshService;
import org.jspecify.annotations.NullMarked;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@NullMarked
public class PolicyRefreshJob {

  private final PolicyRefreshService policyRefreshService;

  @Scheduled(fixedDelayString = "${janus.sidecar.policy-refresh-interval}")
  public void run() {
    policyRefreshService.refreshAllRegisteredPolicies();
  }
}
