package com.academconnect.mapper;

import com.academconnect.domain.AreaTematica;
import com.academconnect.dto.AreaTematicaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AreaTematicaMapper {

    @Mapping(source = "parent.id", target = "parentId")
    AreaTematicaResponse toResponse(AreaTematica area);
}
