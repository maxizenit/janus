package org.janus.sidecar.client.statestore;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.janus.api.statestore.DegradationState;
import org.janus.api.statestore.GetDegradationStatesRequest;
import org.janus.api.statestore.StateStoreServiceGrpc;
import org.janus.sidecar.model.snapshot.StateSnapshot;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@NullMarked
public class GrpcStateStoreClient implements StateStoreClient {

  private final StateStoreServiceGrpc.StateStoreServiceBlockingStub stateStoreStub;

  @Override
  public Map<String, StateSnapshot> getStates(Set<String> degradationIds) {
    if (degradationIds.isEmpty()) {
      return Collections.emptyMap();
    }

    var request =
        GetDegradationStatesRequest.newBuilder().addAllDegradationIds(degradationIds).build();
    var response = stateStoreStub.getDegradationStates(request);

    return response.getDegradationStatesList().stream()
        .collect(Collectors.toMap(DegradationState::getDegradationId, this::toSnapshot));
  }

  private StateSnapshot toSnapshot(DegradationState state) {
    return new StateSnapshot(state.getDegradationId(), state.getValue(), Instant.now(), false);
  }
}
