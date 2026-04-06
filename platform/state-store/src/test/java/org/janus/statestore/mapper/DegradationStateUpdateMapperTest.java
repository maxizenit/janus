package org.janus.statestore.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.util.Durations;
import java.time.Duration;
import org.janus.statestore.model.DegradationState;
import org.janus.statestore.model.DegradationStateUpdate;
import org.janus.statestore.model.DegradationStateUpdateSource;
import org.junit.jupiter.api.Test;

class DegradationStateUpdateMapperTest {

  private final DegradationStateUpdateSourceMapper sourceMapper =
      new DegradationStateUpdateSourceMapper();
  private final DegradationStateUpdateMapper mapper =
      new DegradationStateUpdateMapper(sourceMapper);

  @Test
  void fromGrpcToDomain_validUpdate() {
    org.janus.api.statestore.DegradationStateUpdate grpcUpdate =
        org.janus.api.statestore.DegradationStateUpdate.newBuilder()
            .setDegradationId("deg-1")
            .setValue(0.5)
            .setTtl(Durations.fromSeconds(300))
            .build();

    DegradationStateUpdate result =
        mapper.fromGrpcToDomain(
            grpcUpdate, org.janus.api.statestore.DegradationStateUpdateSource.ADMIN_UI);

    assertThat(result.degradationId()).isEqualTo("deg-1");
    assertThat(result.value()).isEqualTo(0.5);
    assertThat(result.source()).isEqualTo(DegradationStateUpdateSource.ADMIN_UI);
    assertThat(result.ttl()).isEqualTo(Duration.ofSeconds(300));
  }

  @Test
  void fromGrpcToDomain_unrecognizedSource_throws() {
    org.janus.api.statestore.DegradationStateUpdate grpcUpdate =
        org.janus.api.statestore.DegradationStateUpdate.newBuilder()
            .setDegradationId("deg-1")
            .setValue(0.5)
            .setTtl(Durations.fromSeconds(300))
            .build();

    assertThatThrownBy(
            () ->
                mapper.fromGrpcToDomain(
                    grpcUpdate, org.janus.api.statestore.DegradationStateUpdateSource.UNRECOGNIZED))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Source must be recognized");
  }

  @Test
  void fromUpdateToState() {
    DegradationStateUpdate update =
        new DegradationStateUpdate(
            "deg-1", 0.75, DegradationStateUpdateSource.EVALUATOR, Duration.ofSeconds(60));

    DegradationState state = mapper.fromUpdateToState(update);

    assertThat(state.degradationId()).isEqualTo("deg-1");
    assertThat(state.value()).isEqualTo(0.75);
  }

  @Test
  void fromGrpc_duration() {
    com.google.protobuf.Duration grpcDuration = Durations.fromSeconds(120);

    Duration result = mapper.fromGrpc(grpcDuration);

    assertThat(result).isEqualTo(Duration.ofSeconds(120));
  }

  @Test
  void toGrpc_duration() {
    Duration duration = Duration.ofMillis(5000);

    com.google.protobuf.Duration result = mapper.toGrpc(duration);

    assertThat(Durations.toMillis(result)).isEqualTo(5000);
  }
}
