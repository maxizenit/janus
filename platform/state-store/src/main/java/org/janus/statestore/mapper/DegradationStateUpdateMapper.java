package org.janus.statestore.mapper;

import org.janus.api.statestore.DegradationStateUpdateSource;
import org.janus.api.statestore.StateStoreServiceOuterClass;
import org.janus.statestore.model.DegradationState;
import org.janus.statestore.model.DegradationStateUpdate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = DegradationStateUpdateSourceMapper.class)
public interface DegradationStateUpdateMapper {

    @Mapping(source = "updateSourceGrpcObject", target = "source")
    DegradationStateUpdate fromGrpcToModel(org.janus.api.statestore.DegradationStateUpdate grpcObject,
                                           DegradationStateUpdateSource updateSourceGrpcObject);

    DegradationState fromModelToState(DegradationStateUpdate modelObject);

    default java.time.Duration fromGrpcToJavaDuration(com.google.protobuf.Duration grpcDuration) {
        return grpcDuration == null
               ? null
               : java.time.Duration.ofMillis(com.google.protobuf.util.Durations.toMillis(grpcDuration));
    }
}
