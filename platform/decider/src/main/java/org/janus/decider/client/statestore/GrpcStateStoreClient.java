package org.janus.decider.client.statestore;

import com.google.protobuf.util.Durations;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.api.statestore.DegradationStateUpdate;
import org.janus.api.statestore.DegradationStateUpdateSource;
import org.janus.api.statestore.StateStoreServiceGrpc;
import org.janus.api.statestore.UpdateDegradationStatesRequest;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class GrpcStateStoreClient implements StateStoreClient {

  private final StateStoreServiceGrpc.StateStoreServiceBlockingStub stateStoreStub;

  @Override
  public void updateState(String degradationId, double value, Duration ttl) {
    log.debug(
        "Updating state in state store: degradation={}, value={}, ttl={}",
        degradationId,
        value,
        ttl);

    var request =
        UpdateDegradationStatesRequest.newBuilder()
            .setSource(DegradationStateUpdateSource.DECIDER)
            .addUpdates(
                DegradationStateUpdate.newBuilder()
                    .setDegradationId(degradationId)
                    .setValue(value)
                    .setTtl(Durations.fromMillis(ttl.toMillis()))
                    .build())
            .build();
    stateStoreStub.updateDegradationStates(request);

    log.debug(
        "State store update completed: degradation={}, value={}, ttl={}",
        degradationId,
        value,
        ttl);
  }
}
