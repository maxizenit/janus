package org.janus.statestore.api;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.api.statestore.ClearDegradationStateOverrideRequest;
import org.janus.api.statestore.ClearDegradationStateOverrideResponse;
import org.janus.api.statestore.GetAdminDegradationStatesRequest;
import org.janus.api.statestore.GetAdminDegradationStatesResponse;
import org.janus.api.statestore.GetDegradationStatesRequest;
import org.janus.api.statestore.GetDegradationStatesResponse;
import org.janus.api.statestore.StateStoreServiceGrpc;
import org.janus.api.statestore.UpdateDegradationStatesRequest;
import org.janus.api.statestore.UpdateDegradationStatesResponse;
import org.janus.statestore.mapper.DegradationStateMapper;
import org.janus.statestore.mapper.DegradationStateUpdateMapper;
import org.janus.statestore.mapper.DegradationStateUpdateSourceMapper;
import org.janus.statestore.model.DegradationStateUpdate;
import org.janus.statestore.model.DegradationStateUpdateSource;
import org.janus.statestore.service.DegradationStateService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class StateStoreGrpcApi extends StateStoreServiceGrpc.StateStoreServiceImplBase {

  private final DegradationStateService stateService;
  private final DegradationStateMapper stateMapper;
  private final DegradationStateUpdateMapper updateMapper;
  private final DegradationStateUpdateSourceMapper sourceMapper;

  @Override
  public void getDegradationStates(
      GetDegradationStatesRequest request,
      StreamObserver<GetDegradationStatesResponse> responseObserver) {
    var degradationIds = request.getDegradationIdsList();
    log.debug("GetDegradationStates request received: degradationCount={}", degradationIds.size());

    var states =
        stateService.getDegradationStates(degradationIds).stream()
            .map(stateMapper::toGrpc)
            .toList();

    log.debug(
        "GetDegradationStates request completed: requested={}, returned={}",
        degradationIds.size(),
        states.size());

    responseObserver.onNext(
        GetDegradationStatesResponse.newBuilder().addAllDegradationStates(states).build());
    responseObserver.onCompleted();
  }

  @Override
  public void getAdminDegradationStates(
      GetAdminDegradationStatesRequest request,
      StreamObserver<GetAdminDegradationStatesResponse> responseObserver) {
    var degradationIds = request.getDegradationIdsList();
    log.debug(
        "GetAdminDegradationStates request received: degradationCount={}", degradationIds.size());

    var states =
        stateService.getAdminDegradationStates(degradationIds).stream()
            .map(stateMapper::toGrpc)
            .toList();

    log.debug(
        "GetAdminDegradationStates request completed: requested={}, returned={}",
        degradationIds.size(),
        states.size());

    responseObserver.onNext(
        GetAdminDegradationStatesResponse.newBuilder().addAllDegradationStates(states).build());
    responseObserver.onCompleted();
  }

  @Override
  public void updateDegradationStates(
      UpdateDegradationStatesRequest request,
      StreamObserver<UpdateDegradationStatesResponse> responseObserver) {
    log.info(
        "UpdateDegradationStates request received: source={}, updatesCount={}",
        request.getSource(),
        request.getUpdatesCount());

    DegradationStateUpdateSource source = sourceMapper.fromGrpcToDomain(request.getSource());
    if (source == null) {
      log.warn(
          "UpdateDegradationStates request rejected: invalid source={}, updatesCount={}",
          request.getSource(),
          request.getUpdatesCount());
      responseObserver.onError(
          Status.INVALID_ARGUMENT
              .withDescription("Source must be specified and recognized")
              .asRuntimeException());
      return;
    }

    try {
      List<DegradationStateUpdate> updates =
          request.getUpdatesList().stream()
              .map(update -> updateMapper.fromGrpcToDomain(update, request.getSource()))
              .toList();
      stateService.updateDegradationStates(updates);
      log.info(
          "UpdateDegradationStates request completed: source={}, updatesCount={}",
          request.getSource(),
          updates.size());
    } catch (IllegalArgumentException e) {
      responseObserver.onError(invalidArgument(e));
      return;
    }

    responseObserver.onNext(UpdateDegradationStatesResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void clearDegradationStateOverride(
      ClearDegradationStateOverrideRequest request,
      StreamObserver<ClearDegradationStateOverrideResponse> responseObserver) {
    DegradationStateUpdateSource source = sourceMapper.fromGrpcToDomain(request.getSource());
    if (source == null) {
      log.warn(
          "ClearDegradationStateOverride request rejected: invalid source={}, degradationCount={}",
          request.getSource(),
          request.getDegradationIdsCount());
      responseObserver.onError(
          Status.INVALID_ARGUMENT
              .withDescription("Source must be specified and recognized")
              .asRuntimeException());
      return;
    }

    var degradationIds = request.getDegradationIdsList();

    log.info(
        "ClearDegradationStateOverride request received: source={}, degradationCount={}",
        source,
        degradationIds.size());

    try {
      stateService.clearDegradationStates(degradationIds, source);
    } catch (IllegalArgumentException e) {
      responseObserver.onError(invalidArgument(e));
      return;
    }

    log.info(
        "ClearDegradationStateOverride request completed: source={}, degradationCount={}",
        source,
        degradationIds.size());

    responseObserver.onNext(ClearDegradationStateOverrideResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  private static io.grpc.StatusRuntimeException invalidArgument(IllegalArgumentException e) {
    String message = e.getMessage();
    return Status.INVALID_ARGUMENT
        .withDescription(message == null || message.isBlank() ? "Invalid request" : message)
        .asRuntimeException();
  }
}
