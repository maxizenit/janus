package org.janus.statestore.mapper;

import org.janus.statestore.model.DegradationStateUpdateSource;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class DegradationStateUpdateSourceMapper {

  public @Nullable DegradationStateUpdateSource fromGrpcToDomain(
      org.janus.api.statestore.DegradationStateUpdateSource sourceGrpc) {
    return switch (sourceGrpc) {
      case ADMIN_UI -> DegradationStateUpdateSource.ADMIN_UI;
      case DECIDER -> DegradationStateUpdateSource.DECIDER;
      case UNRECOGNIZED -> null;
    };
  }

  public org.janus.api.statestore.DegradationStateUpdateSource fromDomainToGrpc(
      DegradationStateUpdateSource source) {
    return switch (source) {
      case ADMIN_UI -> org.janus.api.statestore.DegradationStateUpdateSource.ADMIN_UI;
      case DECIDER -> org.janus.api.statestore.DegradationStateUpdateSource.DECIDER;
    };
  }
}
