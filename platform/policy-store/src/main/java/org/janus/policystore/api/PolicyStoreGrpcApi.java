package org.janus.policystore.api;

import io.grpc.stub.StreamObserver;
import org.janus.api.policystore.DegradationMetadata;
import org.janus.api.policystore.DegradationPolicy;
import org.janus.api.policystore.GetDegradationMetadataRequest;
import org.janus.api.policystore.GetDegradationMetadataResponse;
import org.janus.api.policystore.GetDegradationPoliciesRequest;
import org.janus.api.policystore.GetDegradationPoliciesResponse;
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
    public void getDegradationPolicies(GetDegradationPoliciesRequest request,
                                       StreamObserver<GetDegradationPoliciesResponse> responseObserver) {
        List<DegradationPolicy> policies = request.getDegradationIdsList()
                                                  .stream()
                                                  .map(id -> DegradationPolicy.newBuilder()
                                                                              .setDegradationId(id)
                                                                              .build())
                                                  .toList();

        responseObserver.onNext(GetDegradationPoliciesResponse.newBuilder()
                                                              .addAllDegradationPolicies(policies)
                                                              .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getDegradationMetadata(GetDegradationMetadataRequest request,
                                       StreamObserver<GetDegradationMetadataResponse> responseObserver) {
        List<DegradationMetadata> metadata = request.getDegradationIdsList()
                                                    .stream()
                                                    .map(id -> {
                                                        double criticalThreshold = ThreadLocalRandom.current()
                                                                                                    .nextDouble();
                                                        double minFallbackRatio = ThreadLocalRandom.current()
                                                                                                   .nextDouble();
                                                        double maxFallbackRatio = ThreadLocalRandom.current()
                                                                                                   .nextDouble(
                                                                                                           minFallbackRatio,
                                                                                                           1.0);
                                                        return DegradationMetadata.newBuilder()
                                                                                  .setDegradationId(id)
                                                                                  .setCriticalThreshold(
                                                                                          criticalThreshold)
                                                                                  .setMinFallbackRatio(minFallbackRatio)
                                                                                  .setMaxFallbackRatio(maxFallbackRatio)
                                                                                  .build();
                                                    })
                                                    .toList();

        responseObserver.onNext(GetDegradationMetadataResponse.newBuilder()
                                                              .addAllDegradationMetadata(metadata)
                                                              .build());
        responseObserver.onCompleted();
    }
}
