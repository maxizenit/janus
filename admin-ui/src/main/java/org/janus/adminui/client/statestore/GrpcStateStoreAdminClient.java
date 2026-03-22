package org.janus.adminui.client.statestore;

import com.google.protobuf.util.Durations;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.adminui.mapper.StateViewMapper;
import org.janus.adminui.model.OverrideStateCommand;
import org.janus.adminui.model.StateView;
import org.janus.api.statestore.ClearDegradationStateOverrideRequest;
import org.janus.api.statestore.DegradationStateUpdate;
import org.janus.api.statestore.DegradationStateUpdateSource;
import org.janus.api.statestore.GetAdminDegradationStatesRequest;
import org.janus.api.statestore.StateStoreServiceGrpc;
import org.janus.api.statestore.UpdateDegradationStatesRequest;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class GrpcStateStoreAdminClient implements StateStoreAdminClient {

  private final StateStoreServiceGrpc.StateStoreServiceBlockingStub stub;
  private final StateViewMapper mapper;
  private final Clock clock;

  @Override
  public List<StateView> getAdminStates(List<String> degradationIds) {
    if (degradationIds.isEmpty()) {
      log.debug("Skipping state load: empty degradationIds");
      return List.of();
    }

    log.debug("Loading admin states: degradationCount={}", degradationIds.size());

    Instant refreshedAt = clock.instant();
    var response =
        stub.getAdminDegradationStates(
            GetAdminDegradationStatesRequest.newBuilder()
                .addAllDegradationIds(degradationIds)
                .build());

    log.debug(
        "Admin states loaded: requested={}, returned={}",
        degradationIds.size(),
        response.getDegradationStatesCount());

    return response.getDegradationStatesList().stream()
        .map(state -> mapper.fromGrpc(state, refreshedAt))
        .toList();
  }

  @Override
  public void applyOverride(OverrideStateCommand command) {
    log.info(
        "Applying override: degradationId={}, value={}, ttl={}",
        command.degradationId(),
        command.value(),
        command.ttl());

    stub.updateDegradationStates(
        UpdateDegradationStatesRequest.newBuilder()
            .setSource(DegradationStateUpdateSource.ADMIN_UI)
            .addUpdates(
                DegradationStateUpdate.newBuilder()
                    .setDegradationId(command.degradationId())
                    .setValue(command.value())
                    .setTtl(Durations.fromMillis(command.ttl().toMillis()))
                    .build())
            .build());

    log.info("Override applied: degradationId={}", command.degradationId());
  }

  @Override
  public void clearOverride(String degradationId) {
    log.info("Clearing override: degradationId={}", degradationId);

    stub.clearDegradationStateOverride(
        ClearDegradationStateOverrideRequest.newBuilder()
            .setSource(DegradationStateUpdateSource.ADMIN_UI)
            .addDegradationIds(degradationId)
            .build());

    log.info("Override cleared: degradationId={}", degradationId);
  }
}
