package org.janus.sidecar.client.statestore;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.api.statestore.DegradationState;
import org.janus.api.statestore.GetDegradationStatesRequest;
import org.janus.api.statestore.StateStoreServiceGrpc;
import org.janus.sidecar.model.snapshot.StateSnapshot;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class GrpcStateStoreClient implements StateStoreClient {

  private final StateStoreServiceGrpc.StateStoreServiceBlockingStub stateStoreStub;

  @Override
  public Map<String, StateSnapshot> getStates(Set<String> degradationIds) {
    if (degradationIds.isEmpty()) {
      log.debug("Skipping state store lookup: empty degradationIds");
      return Collections.emptyMap();
    }

    log.debug("Requesting states from state store: requested={}", degradationIds.size());

    var request =
        GetDegradationStatesRequest.newBuilder().addAllDegradationIds(degradationIds).build();
    var response = stateStoreStub.getDegradationStates(request);
    var states =
        response.getDegradationStatesList().stream()
            .collect(Collectors.toMap(DegradationState::getDegradationId, this::toSnapshot));

    log.debug(
        "States received from state store: requested={}, returned={}",
        degradationIds.size(),
        states.size());

    var missingIds = new HashSet<>(degradationIds);
    missingIds.removeAll(states.keySet());
    if (!missingIds.isEmpty()) {
      log.debug(
          "State store response is partial: missingCount={}, missingIds={}",
          missingIds.size(),
          missingIds.size() <= 20 ? missingIds : "[omitted]");
    }

    return states;
  }

  private StateSnapshot toSnapshot(DegradationState state) {
    return new StateSnapshot(state.getDegradationId(), state.getValue(), Instant.now(), false);
  }
}
