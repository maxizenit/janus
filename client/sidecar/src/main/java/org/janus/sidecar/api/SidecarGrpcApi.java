package org.janus.sidecar.api;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.janus.api.sidecar.SidecarServiceGrpc;
import org.janus.api.sidecar.SidecarServiceOuterClass;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@NullMarked
public class SidecarGrpcApi extends SidecarServiceGrpc.SidecarServiceImplBase {

    @Override
    public void registerActualDegradations(SidecarServiceOuterClass.RegisterActualDegradationsRequest request,
                                           StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void getDegradations(Empty request,
                                StreamObserver<SidecarServiceOuterClass.GetDegradationsResponse> responseObserver) {
        responseObserver.onNext(SidecarServiceOuterClass.GetDegradationsResponse.newBuilder()
                                                                                .build());
        responseObserver.onCompleted();
    }
}
