package org.janus.sidecar.api;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.janus.api.sidecar.GetDegradationsRequest;
import org.janus.api.sidecar.GetDegradationsResponse;
import org.janus.api.sidecar.SidecarServiceGrpc;
import org.janus.api.sidecar.SyncActualDegradationsRequest;
import org.janus.api.sidecar.SyncActualDegradationsResponse;
import org.janus.sidecar.mapper.SidecarGrpcMapper;
import org.janus.sidecar.service.handler.GetDegradationsHandler;
import org.janus.sidecar.service.handler.SyncActualDegradationsHandler;
import org.janus.sidecar.validation.ActualDegradationIdsValidator;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@NullMarked
public class SidecarGrpcApi extends SidecarServiceGrpc.SidecarServiceImplBase {

  private final SyncActualDegradationsHandler syncHandler;
  private final GetDegradationsHandler getHandler;
  private final SidecarGrpcMapper grpcMapper;
  private final ActualDegradationIdsValidator actualDegradationIdsValidator;

  @Override
  public void syncActualDegradations(
      SyncActualDegradationsRequest request,
      StreamObserver<SyncActualDegradationsResponse> responseObserver) {
    try {
      actualDegradationIdsValidator.validate(request.getDegradationIdsList());
    } catch (IllegalArgumentException e) {
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
      return;
    }

    var command = grpcMapper.fromSyncActualDegradationsRequestToResponse(request);
    syncHandler.handle(command);

    responseObserver.onNext(SyncActualDegradationsResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void getDegradations(
      GetDegradationsRequest request, StreamObserver<GetDegradationsResponse> responseObserver) {
    var result = getHandler.handle();
    var response = grpcMapper.fromDegradationViewsToGetDegradationsResponse(result);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
