package org.janus.sidecar.api;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.janus.api.sidecar.GetDegradationsResponse;
import org.janus.api.sidecar.RegisterActualDegradationsRequest;
import org.janus.api.sidecar.SidecarServiceGrpc;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@NullMarked
public class SidecarGrpcApi extends SidecarServiceGrpc.SidecarServiceImplBase {

    @Override
    public void registerActualDegradations(RegisterActualDegradationsRequest request,
                                           StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void getDegradations(Empty request, StreamObserver<GetDegradationsResponse> responseObserver) {
        responseObserver.onNext(GetDegradationsResponse.newBuilder()
                                                       .build());
        responseObserver.onCompleted();
    }
}
