package org.janus.decider.scheduling;

import lombok.RequiredArgsConstructor;
import org.janus.decider.service.PolicyRefreshService;
import org.jspecify.annotations.NullMarked;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@NullMarked
public class PolicyRefreshJob {

  private final PolicyRefreshService policyRefreshService;

  @Scheduled(fixedDelayString = "${janus.decider.policy-refresh-interval}")
  public void refreshPolicies() {
    policyRefreshService.refreshAllPolicies();
  }
}
