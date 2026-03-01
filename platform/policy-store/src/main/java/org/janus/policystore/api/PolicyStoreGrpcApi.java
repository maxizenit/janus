package org.janus.policystore.api;

import io.grpc.stub.StreamObserver;
import org.janus.api.policystore.PolicyStoreServiceGrpc;
import org.janus.api.policystore.PolicyStoreServiceOuterClass;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@NullMarked
public class PolicyStoreGrpcApi extends PolicyStoreServiceGrpc.PolicyStoreServiceImplBase {

    @Override
    public void getDegradationPolicies(PolicyStoreServiceOuterClass.GetDegradationPoliciesRequest request,
                                       StreamObserver<PolicyStoreServiceOuterClass.GetDegradationPoliciesResponse> responseObserver) {
        List<PolicyStoreServiceOuterClass.DegradationPolicy> policies = request.getDegradationIdsList()
                                                                               .stream()
                                                                               .map(id -> PolicyStoreServiceOuterClass.DegradationPolicy.newBuilder()
                                                                                                                                        .setDegradationId(
                                                                                                                                                id)
                                                                                                                                        .build())
                                                                               .toList();

        responseObserver.onNext(PolicyStoreServiceOuterClass.GetDegradationPoliciesResponse.newBuilder()
                                                                                           .addAllDegradationPolicies(
                                                                                                   policies)
                                                                                           .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getDegradationMetadata(PolicyStoreServiceOuterClass.GetDegradationMetadataRequest request,
                                       StreamObserver<PolicyStoreServiceOuterClass.GetDegradationMetadataResponse> responseObserver) {
        List<PolicyStoreServiceOuterClass.DegradationMetadata> metadata = request.getDegradationIdsList()
                                                                                 .stream()
                                                                                 .map(id -> {
                                                                                     double criticalThreshold =
                                                                                             ThreadLocalRandom.current()
                                                                                                              .nextDouble();
                                                                                     double minFallbackRatio =
                                                                                             ThreadLocalRandom.current()
                                                                                                              .nextDouble();
                                                                                     double maxFallbackRatio =
                                                                                             ThreadLocalRandom.current()
                                                                                                              .nextDouble(
                                                                                                                      minFallbackRatio,
                                                                                                                      1.0);
                                                                                     return PolicyStoreServiceOuterClass.DegradationMetadata.newBuilder()
                                                                                                                                            .setDegradationId(
                                                                                                                                                    id)
                                                                                                                                            .setCriticalThreshold(
                                                                                                                                                    criticalThreshold)
                                                                                                                                            .setMinFallbackRatio(
                                                                                                                                                    minFallbackRatio)
                                                                                                                                            .setMaxFallbackRatio(
                                                                                                                                                    maxFallbackRatio)
                                                                                                                                            .build();
                                                                                 })
                                                                                 .toList();

        responseObserver.onNext(PolicyStoreServiceOuterClass.GetDegradationMetadataResponse.newBuilder()
                                                                                           .addAllDegradationMetadata(
                                                                                                   metadata)
                                                                                           .build());
        responseObserver.onCompleted();
    }
}
