package org.janus.statestore.mapper;

import com.google.protobuf.util.Durations;
import java.time.Duration;
import org.janus.statestore.model.AdminDegradationState;
import org.janus.statestore.model.DegradationState;
import org.janus.statestore.model.EffectiveDegradationState;
import org.janus.statestore.model.SourceDegradationState;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class DegradationStateMapper {

  private final DegradationStateUpdateSourceMapper sourceMapper;

  public DegradationStateMapper(DegradationStateUpdateSourceMapper sourceMapper) {
    this.sourceMapper = sourceMapper;
  }

  public org.janus.api.statestore.DegradationState toGrpc(DegradationState state) {
    return org.janus.api.statestore.DegradationState.newBuilder()
        .setDegradationId(state.degradationId())
        .setValue(state.value())
        .build();
  }

  public org.janus.api.statestore.AdminDegradationState toGrpc(AdminDegradationState state) {
    EffectiveDegradationState effectiveState = state.effectiveState();

    var builder =
        org.janus.api.statestore.AdminDegradationState.newBuilder()
            .setDegradationId(state.degradationId())
            .setEffectiveValue(effectiveState.value())
            .setEffectiveSource(sourceMapper.fromDomainToGrpc(effectiveState.source()));

    for (SourceDegradationState sourceState : state.sourceStates()) {
      builder.addSourceStates(toGrpc(sourceState));
    }

    return builder.build();
  }

  public org.janus.api.statestore.DegradationStateSourceEntry toGrpc(SourceDegradationState state) {
    return org.janus.api.statestore.DegradationStateSourceEntry.newBuilder()
        .setSource(sourceMapper.fromDomainToGrpc(state.source()))
        .setValue(state.value())
        .setRemainingTtl(toGrpc(state.remainingTtl()))
        .build();
  }

  private com.google.protobuf.Duration toGrpc(Duration duration) {
    return Durations.fromMillis(duration.toMillis());
  }
}
