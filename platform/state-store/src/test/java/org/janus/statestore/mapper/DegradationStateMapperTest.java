package org.janus.statestore.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.janus.statestore.model.AdminDegradationState;
import org.janus.statestore.model.DegradationState;
import org.janus.statestore.model.DegradationStateUpdateSource;
import org.janus.statestore.model.EffectiveDegradationState;
import org.janus.statestore.model.SourceDegradationState;
import org.junit.jupiter.api.Test;

class DegradationStateMapperTest {

  private final DegradationStateUpdateSourceMapper sourceMapper =
      new DegradationStateUpdateSourceMapper();
  private final DegradationStateMapper mapper = new DegradationStateMapper(sourceMapper);

  @Test
  void toGrpc_degradationState() {
    DegradationState state = new DegradationState("deg-1", 0.75);

    org.janus.api.statestore.DegradationState grpc = mapper.toGrpc(state);

    assertThat(grpc.getDegradationId()).isEqualTo("deg-1");
    assertThat(grpc.getValue()).isEqualTo(0.75);
  }

  @Test
  void toGrpc_adminDegradationState() {
    EffectiveDegradationState effectiveState =
        new EffectiveDegradationState(0.5, DegradationStateUpdateSource.ADMIN_UI);
    SourceDegradationState adminSource =
        new SourceDegradationState(
            DegradationStateUpdateSource.ADMIN_UI, 0.5, Duration.ofSeconds(120));
    SourceDegradationState evaluatorSource =
        new SourceDegradationState(
            DegradationStateUpdateSource.EVALUATOR, 0.8, Duration.ofSeconds(60));
    AdminDegradationState state =
        new AdminDegradationState("deg-2", effectiveState, List.of(adminSource, evaluatorSource));

    org.janus.api.statestore.AdminDegradationState grpc = mapper.toGrpc(state);

    assertThat(grpc.getDegradationId()).isEqualTo("deg-2");
    assertThat(grpc.getEffectiveValue()).isEqualTo(0.5);
    assertThat(grpc.getEffectiveSource())
        .isEqualTo(org.janus.api.statestore.DegradationStateUpdateSource.ADMIN_UI);
    assertThat(grpc.getSourceStatesCount()).isEqualTo(2);

    assertThat(grpc.getSourceStates(0).getSource())
        .isEqualTo(org.janus.api.statestore.DegradationStateUpdateSource.ADMIN_UI);
    assertThat(grpc.getSourceStates(0).getValue()).isEqualTo(0.5);

    assertThat(grpc.getSourceStates(1).getSource())
        .isEqualTo(org.janus.api.statestore.DegradationStateUpdateSource.EVALUATOR);
    assertThat(grpc.getSourceStates(1).getValue()).isEqualTo(0.8);
  }

  @Test
  void toGrpc_sourceDegradationState() {
    SourceDegradationState state =
        new SourceDegradationState(
            DegradationStateUpdateSource.EVALUATOR, 0.9, Duration.ofMillis(5000));

    org.janus.api.statestore.DegradationStateSourceEntry grpc = mapper.toGrpc(state);

    assertThat(grpc.getSource())
        .isEqualTo(org.janus.api.statestore.DegradationStateUpdateSource.EVALUATOR);
    assertThat(grpc.getValue()).isEqualTo(0.9);
    assertThat(grpc.getRemainingTtl().getSeconds()).isEqualTo(5);
  }
}
