package com.academconnect.mapper;

import com.academconnect.domain.Estudiante;
import com.academconnect.dto.EstudianteRequest;
import com.academconnect.dto.EstudianteResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = UsuarioAreaTematicaMapper.class)
public interface EstudianteMapper {

    EstudianteResponse toResponse(Estudiante estudiante);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "areas", ignore = true)
    Estudiante toEntity(EstudianteRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "areas", ignore = true)
    void update(EstudianteRequest request, @MappingTarget Estudiante target);
}
