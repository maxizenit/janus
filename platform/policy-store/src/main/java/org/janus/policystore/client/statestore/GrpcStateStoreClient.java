package org.janus.policystore.client.statestore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.api.statestore.ClearDegradationStatesRequest;
import org.janus.api.statestore.DegradationStateUpdateSource;
import org.janus.api.statestore.StateStoreServiceGrpc;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class GrpcStateStoreClient implements StateStoreClient {

  private final StateStoreServiceGrpc.StateStoreServiceBlockingStub stub;

  @Override
  public void clearAllStates(String degradationId) {
    log.debug("Clearing state for degradation across all sources: degradationId={}", degradationId);

    for (DegradationStateUpdateSource source : DegradationStateUpdateSource.values()) {
      if (source == DegradationStateUpdateSource.DEGRADATION_STATE_UPDATE_SOURCE_UNSPECIFIED
          || source == DegradationStateUpdateSource.UNRECOGNIZED) {
        continue;
      }

      stub.clearDegradationStates(
          ClearDegradationStatesRequest.newBuilder()
              .setSource(source)
              .addDegradationIds(degradationId)
              .build());
    }

    log.debug("State cleared for degradation: degradationId={}", degradationId);
  }
}
