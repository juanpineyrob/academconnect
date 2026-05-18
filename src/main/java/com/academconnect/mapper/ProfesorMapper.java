package com.academconnect.mapper;

import com.academconnect.domain.Profesor;
import com.academconnect.dto.ProfesorRequest;
import com.academconnect.dto.ProfesorResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = UsuarioAreaTematicaMapper.class)
public interface ProfesorMapper {

    ProfesorResponse toResponse(Profesor profesor);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "areas", ignore = true)
    Profesor toEntity(ProfesorRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "areas", ignore = true)
    void update(ProfesorRequest request, @MappingTarget Profesor target);
}
