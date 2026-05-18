package com.academconnect.mapper;

import com.academconnect.domain.SolicitudVinculacion;
import com.academconnect.dto.SolicitudVinculacionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SolicitudVinculacionMapper {

    @Mapping(source = "trabajo.id", target = "trabajoId")
    @Mapping(source = "trabajo.titulo", target = "trabajoTitulo")
    @Mapping(source = "estudiante.id", target = "estudianteId")
    @Mapping(source = "estudiante.nombre", target = "estudianteNombre")
    SolicitudVinculacionResponse toResponse(SolicitudVinculacion solicitud);
}
