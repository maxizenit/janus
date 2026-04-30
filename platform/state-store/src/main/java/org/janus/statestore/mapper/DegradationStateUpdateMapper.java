package org.janus.statestore.mapper;

import com.google.protobuf.util.Durations;
import java.time.Duration;
import org.janus.statestore.model.DegradationState;
import org.janus.statestore.model.DegradationStateUpdate;
import org.janus.statestore.model.DegradationStateUpdateSource;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class DegradationStateUpdateMapper {

  private final DegradationStateUpdateSourceMapper sourceMapper;

  public DegradationStateUpdateMapper(DegradationStateUpdateSourceMapper sourceMapper) {
    this.sourceMapper = sourceMapper;
  }

  public DegradationStateUpdate fromGrpcToDomain(
      org.janus.api.statestore.DegradationStateUpdate updateGrpc,
      org.janus.api.statestore.DegradationStateUpdateSource sourceGrpc) {
    DegradationStateUpdateSource source = sourceMapper.fromGrpcToDomain(sourceGrpc);
    if (source == null) {
      throw new IllegalArgumentException("Source must be recognized");
    }

    return new DegradationStateUpdate(
        updateGrpc.getDegradationId(),
        updateGrpc.getValue(),
        source,
        fromGrpc(updateGrpc.getTtl()));
  }

  public DegradationState fromUpdateToState(DegradationStateUpdate update) {
    return new DegradationState(update.degradationId(), update.value());
  }

  public Duration fromGrpc(com.google.protobuf.Duration durationGrpc) {
    return Duration.ofMillis(Durations.toMillis(durationGrpc));
  }

  public com.google.protobuf.Duration toGrpc(Duration duration) {
    return Durations.fromMillis(duration.toMillis());
  }
}
