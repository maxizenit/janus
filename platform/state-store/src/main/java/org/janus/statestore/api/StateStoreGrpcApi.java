package org.janus.statestore.api;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.janus.api.statestore.DegradationState;
import org.janus.api.statestore.GetDegradationStatesRequest;
import org.janus.api.statestore.GetDegradationStatesResponse;
import org.janus.api.statestore.StateStoreServiceGrpc;
import org.janus.api.statestore.StateStoreServiceOuterClass;
import org.janus.api.statestore.UpdateDegradationStatesRequest;
import org.janus.statestore.mapper.DegradationStateMapper;
import org.janus.statestore.mapper.DegradationStateUpdateMapper;
import org.janus.statestore.model.DegradationStateUpdate;
import org.janus.statestore.service.DegradationStateService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@NullMarked
public class StateStoreGrpcApi extends StateStoreServiceGrpc.StateStoreServiceImplBase {

    private final DegradationStateService stateService;
    private final DegradationStateMapper stateMapper;
    private final DegradationStateUpdateMapper updateMapper;

    @Override
    public void getDegradationStates(GetDegradationStatesRequest request,
                                     StreamObserver<GetDegradationStatesResponse> responseObserver) {
        List<DegradationState> states = stateService.getDegradationStates(request.getDegradationIdsList())
                                                    .stream()
                                                    .map(stateMapper::fromModelToGrpc)
                                                    .toList();

        responseObserver.onNext(GetDegradationStatesResponse.newBuilder()
                                                            .addAllDegradationStates(states)
                                                            .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getDegradationStatesWithAllSources(GetDegradationStatesRequest request,
                                                   StreamObserver<GetDegradationStatesResponse> responseObserver) {
        //TODO: implementation
        super.getDegradationStatesWithAllSources(request, responseObserver);
    }

    @Override
    public void updateDegradationStates(UpdateDegradationStatesRequest request,
                                        StreamObserver<Empty> responseObserver) {
        List<DegradationStateUpdate> updates = request.getUpdatesList()
                                                      .stream()
                                                      .map(update -> updateMapper.fromGrpcToModel(update,
                                                                                                  request.getSource()))
                                                      .toList();
        stateService.updateDegradationStates(updates);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
