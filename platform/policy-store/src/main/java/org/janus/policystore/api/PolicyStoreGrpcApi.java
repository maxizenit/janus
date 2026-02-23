package org.janus.policystore.api;

import io.grpc.stub.StreamObserver;
import org.janus.api.policystore.PolicyStoreServiceGrpc;
import org.janus.api.policystore.PolicyStoreServiceOuterClass;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@NullMarked
public class PolicyStoreGrpcApi extends PolicyStoreServiceGrpc.PolicyStoreServiceImplBase {

    @Override
    public void getDegradationMetadata(PolicyStoreServiceOuterClass.GetDegradationMetadataRequest request,
                                       StreamObserver<PolicyStoreServiceOuterClass.GetDegradationMetadataResponse> responseObserver) {
        double criticalThreshold = ThreadLocalRandom.current()
                                                    .nextDouble();
        double minFallbackRatio = ThreadLocalRandom.current()
                                                   .nextDouble();
        double maxFallbackRatio = ThreadLocalRandom.current()
                                                   .nextDouble(minFallbackRatio, 1.0);

        responseObserver.onNext(PolicyStoreServiceOuterClass.GetDegradationMetadataResponse.newBuilder()
                                                                                           .setId(request.getId())
                                                                                           .setCriticalThreshold(
                                                                                                   criticalThreshold)
                                                                                           .setMinFallbackRatio(
                                                                                                   minFallbackRatio)
                                                                                           .setMaxFallbackRatio(
                                                                                                   maxFallbackRatio)
                                                                                           .build());
        responseObserver.onCompleted();
    }
}
