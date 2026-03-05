package org.janus.statestore.mapper;

import org.janus.api.statestore.StateStoreServiceOuterClass;
import org.janus.statestore.model.DegradationState;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DegradationStateMapper {

    org.janus.api.statestore.DegradationState fromModelToGrpc(DegradationState modelObject);
}
