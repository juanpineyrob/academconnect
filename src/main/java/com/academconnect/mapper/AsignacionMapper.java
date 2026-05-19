package com.academconnect.mapper;

import com.academconnect.domain.Asignacion;
import com.academconnect.dto.AsignacionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AsignacionMapper {

    @Mapping(source = "trabajo.id", target = "trabajoId")
    @Mapping(source = "trabajo.titulo", target = "trabajoTitulo")
    @Mapping(source = "versionamiento.id", target = "versionamientoId")
    @Mapping(source = "versionamiento.numeroVersion", target = "versionNumero")
    @Mapping(source = "evaluador.id", target = "evaluadorId")
    @Mapping(source = "evaluador.nombre", target = "evaluadorNombre")
    AsignacionResponse toResponse(Asignacion asignacion);
}
