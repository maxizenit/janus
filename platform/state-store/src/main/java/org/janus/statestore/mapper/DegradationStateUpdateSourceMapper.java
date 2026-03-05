package org.janus.statestore.mapper;

import org.janus.api.statestore.StateStoreServiceOuterClass;
import org.janus.statestore.model.DegradationStateUpdateSource;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DegradationStateUpdateSourceMapper {

    @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.NULL)
    DegradationStateUpdateSource fromGrpcToModel(StateStoreServiceOuterClass.DegradationStateUpdateSource grpcObject);
}
