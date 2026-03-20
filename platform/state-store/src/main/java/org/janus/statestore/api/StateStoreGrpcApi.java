package org.janus.statestore.api;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.api.statestore.GetDegradationStatesRequest;
import org.janus.api.statestore.GetDegradationStatesResponse;
import org.janus.api.statestore.StateStoreServiceGrpc;
import org.janus.api.statestore.UpdateDegradationStatesRequest;
import org.janus.api.statestore.UpdateDegradationStatesResponse;
import org.janus.statestore.mapper.DegradationStateMapper;
import org.janus.statestore.mapper.DegradationStateUpdateMapper;
import org.janus.statestore.model.DegradationStateUpdate;
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

  @Override
  public void getDegradationStates(
      GetDegradationStatesRequest request,
      StreamObserver<GetDegradationStatesResponse> responseObserver) {
    var degradationIds = request.getDegradationIdsList();
    log.debug("GetDegradationStates request received: degradationCount={}", degradationIds.size());

    var states =
        stateService.getDegradationStates(degradationIds).stream()
            .map(stateMapper::fromStateToStateGrpc)
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
  public void getDegradationStatesWithAllSources(
      GetDegradationStatesRequest request,
      StreamObserver<GetDegradationStatesResponse> responseObserver) {
    log.warn(
        "GetDegradationStatesWithAllSources request rejected: method is not implemented, degradationCount={}",
        request.getDegradationIdsCount());
    responseObserver.onError(
        Status.UNIMPLEMENTED
            .withDescription("GetDegradationStatesWithAllSources is not implemented")
            .asRuntimeException());
  }

  @Override
  public void updateDegradationStates(
      UpdateDegradationStatesRequest request,
      StreamObserver<UpdateDegradationStatesResponse> responseObserver) {
    log.info(
        "UpdateDegradationStates request received: source={}, updatesCount={}",
        request.getSource(),
        request.getUpdatesCount());

    List<DegradationStateUpdate> updates =
        request.getUpdatesList().stream()
            .map(update -> updateMapper.fromUpdateGrpcToUpdate(update, request.getSource()))
            .toList();
    stateService.updateDegradationStates(updates);

    log.info(
        "UpdateDegradationStates request completed: source={}, updatesCount={}",
        request.getSource(),
        updates.size());

    responseObserver.onNext(UpdateDegradationStatesResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }
}
