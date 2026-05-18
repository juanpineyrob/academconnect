package com.academconnect.mapper;

import com.academconnect.domain.Trabajo;
import com.academconnect.dto.TrabajoRequest;
import com.academconnect.dto.TrabajoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = AreaTematicaMapper.class)
public interface TrabajoMapper {

    @Mapping(source = "orientador.id", target = "orientadorId")
    @Mapping(source = "orientador.nombre", target = "orientadorNombre")
    @Mapping(source = "estudiante.id", target = "estudianteId")
    @Mapping(source = "estudiante.nombre", target = "estudianteNombre")
    TrabajoResponse toResponse(Trabajo trabajo);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "orientador", ignore = true)
    @Mapping(target = "estudiante", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "areas", ignore = true)
    @Mapping(target = "coorientadores", ignore = true)
    @Mapping(target = "solicitudes", ignore = true)
    Trabajo toEntity(TrabajoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "orientador", ignore = true)
    @Mapping(target = "estudiante", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "areas", ignore = true)
    @Mapping(target = "coorientadores", ignore = true)
    @Mapping(target = "solicitudes", ignore = true)
    void update(TrabajoRequest request, @MappingTarget Trabajo target);
}
