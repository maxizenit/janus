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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@NullMarked
public class StateStoreGrpcApi extends StateStoreServiceGrpc.StateStoreServiceImplBase {

    private final Map<String, Double> degradationValues = new ConcurrentHashMap<>();

    @Override
    public void getDegradationValues(StateStoreServiceOuterClass.GetDegradationValuesRequest request,
                                     StreamObserver<StateStoreServiceOuterClass.GetDegradationValuesResponse> responseObserver) {
        List<String> ids = request.getIdsList();
        Map<String, Double> result;
        if (ids.isEmpty()) {
            result = Collections.emptyMap();
        } else {
            result = ids.stream()
                        .filter(degradationValues::containsKey)
                        .collect(Collectors.toMap(Function.identity(), degradationValues::get));
        }


        responseObserver.onNext(StateStoreServiceOuterClass.GetDegradationValuesResponse.newBuilder()
                                                                                        .putAllValuesById(result)
                                                                                        .build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateDegradationValue(StateStoreServiceOuterClass.UpdateDegradationValueRequest request,
                                       StreamObserver<Empty> responseObserver) {
        degradationValues.put(request.getId(), request.getValue());
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
