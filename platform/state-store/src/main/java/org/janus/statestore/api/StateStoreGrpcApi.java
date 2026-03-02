package org.janus.statestore.api;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.janus.api.statestore.StateStoreServiceGrpc;
import org.janus.api.statestore.StateStoreServiceOuterClass;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@NullMarked
public class StateStoreGrpcApi extends StateStoreServiceGrpc.StateStoreServiceImplBase {

    private final Map<String, Double> degradationValues = new ConcurrentHashMap<>();

    @Override
    public void getDegradationStates(StateStoreServiceOuterClass.GetDegradationStatesRequest request,
                                     StreamObserver<StateStoreServiceOuterClass.GetDegradationStatesResponse> responseObserver) {
        List<String> ids = request.getDegradationIdsList();
        List<StateStoreServiceOuterClass.DegradationState> states = ids.stream()
                                                                       .map(id -> {
                                                                           Double value = degradationValues.get(id);
                                                                           if (value == null) {
                                                                               return null;
                                                                           }
                                                                           return StateStoreServiceOuterClass.DegradationState.newBuilder()
                                                                                                                              .setDegradationId(
                                                                                                                                      id)
                                                                                                                              .setValue(
                                                                                                                                      value)
                                                                                                                              .build();
                                                                       })
                                                                       .filter(Objects::nonNull)
                                                                       .toList();

        responseObserver.onNext(StateStoreServiceOuterClass.GetDegradationStatesResponse.newBuilder()
                                                                                        .addAllDegradationStates(states)
                                                                                        .build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateDegradationStates(StateStoreServiceOuterClass.UpdateDegradationStatesRequest request,
                                       StreamObserver<Empty> responseObserver) {
        request.getUpdatesList()
               .forEach(update -> degradationValues.put(update.getDegradationId(), update.getValue()));
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
