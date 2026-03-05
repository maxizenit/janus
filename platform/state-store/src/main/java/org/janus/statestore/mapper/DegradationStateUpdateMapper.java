package org.janus.statestore.mapper;

import org.janus.statestore.model.DegradationState;
import org.janus.statestore.model.DegradationStateUpdate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.Duration;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = DegradationStateUpdateSourceMapper.class)
public interface DegradationStateUpdateMapper {

    @Mapping(source = "sourceGrpc", target = "source")
    DegradationStateUpdate fromUpdateGrpcToUpdate(org.janus.api.statestore.DegradationStateUpdate updateGrpc,
                                                  org.janus.api.statestore.DegradationStateUpdateSource sourceGrpc);

    DegradationState fromUpdateToState(DegradationStateUpdate update);

    default Duration fromGrpcDurationToJavaDuration(com.google.protobuf.Duration grpcDuration) {
        return grpcDuration == null
               ? null
               : Duration.ofMillis(com.google.protobuf.util.Durations.toMillis(grpcDuration));
    }
}
