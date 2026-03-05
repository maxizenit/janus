package org.janus.statestore.mapper;

import org.janus.statestore.model.DegradationState;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DegradationStateMapper {

    @Mapping(target = "unknownFields", ignore = true)
    @Mapping(target = "mergeUnknownFields", ignore = true)
    @Mapping(target = "mergeFrom", ignore = true)
    @Mapping(target = "degradationIdBytes", ignore = true)
    @Mapping(target = "clearOneof", ignore = true)
    @Mapping(target = "clearField", ignore = true)
    @Mapping(target = "allFields", ignore = true)
    org.janus.api.statestore.DegradationState fromStateToStateGrpc(DegradationState state);
}
