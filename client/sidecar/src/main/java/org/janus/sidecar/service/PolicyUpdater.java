package org.janus.sidecar.service;

import lombok.RequiredArgsConstructor;
import org.janus.api.policystore.PolicyStoreServiceGrpc;
import org.janus.api.policystore.PolicyStoreServiceOuterClass;
import org.jspecify.annotations.NullMarked;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@NullMarked
public class PolicyUpdater {

    private final PolicyStoreServiceGrpc.PolicyStoreServiceBlockingStub policyStoreStub;

    @Scheduled(fixedDelayString = "${janus.sidecar.policy-refresh-interval}")
    public void updatePolicies() {
        policyStoreStub.getDegradationPolicies(PolicyStoreServiceOuterClass.GetDegradationPoliciesRequest.newBuilder()
                                                                                                         .build());
    }
}
