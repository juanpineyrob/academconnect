package com.academconnect.mapper;

import com.academconnect.domain.SolicitudEvaluacion;
import com.academconnect.dto.SolicitudEvaluacionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SolicitudEvaluacionMapper {

    @Mapping(source = "trabajo.id", target = "trabajoId")
    @Mapping(source = "trabajo.titulo", target = "trabajoTitulo")
    @Mapping(source = "invitado.id", target = "invitadoId")
    @Mapping(source = "invitado.nombre", target = "invitadoNombre")
    SolicitudEvaluacionResponse toResponse(SolicitudEvaluacion entity);
}
