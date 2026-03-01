package org.janus.decider.service;

import lombok.RequiredArgsConstructor;
import org.janus.api.policystore.PolicyStoreServiceGrpc;
import org.janus.api.policystore.PolicyStoreServiceOuterClass;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PolicyUpdater {

    private final PolicyStoreServiceGrpc.PolicyStoreServiceBlockingStub policyStoreStub;

    @Scheduled(fixedDelayString = "${janus.decider.policy-refresh-interval}")
    public void updatePolicies() {
        policyStoreStub.getDegradationPolicies(PolicyStoreServiceOuterClass.GetDegradationPoliciesRequest.newBuilder()
                                                                                                         .build());
    }
}
