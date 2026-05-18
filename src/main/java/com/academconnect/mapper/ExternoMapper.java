package com.academconnect.mapper;

import com.academconnect.domain.Externo;
import com.academconnect.dto.ExternoRequest;
import com.academconnect.dto.ExternoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = UsuarioAreaTematicaMapper.class)
public interface ExternoMapper {

    ExternoResponse toResponse(Externo externo);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "areas", ignore = true)
    Externo toEntity(ExternoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "areas", ignore = true)
    void update(ExternoRequest request, @MappingTarget Externo target);
}
