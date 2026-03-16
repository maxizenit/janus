package org.janus.decider.service;

import lombok.RequiredArgsConstructor;
import org.janus.api.policystore.GetDeciderDegradationPoliciesRequest;
import org.janus.api.policystore.PolicyStoreServiceGrpc;
import org.jspecify.annotations.NullMarked;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@NullMarked
public class PolicyUpdater {

  private final PolicyStoreServiceGrpc.PolicyStoreServiceBlockingStub policyStoreStub;

  @Scheduled(fixedDelayString = "${janus.decider.policy-refresh-interval}")
  public void updatePolicies() {
    policyStoreStub.getDeciderDegradationPolicies(
        GetDeciderDegradationPoliciesRequest.newBuilder().build());
  }
}
