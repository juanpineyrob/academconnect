package com.academconnect.mapper;

import com.academconnect.domain.SolicitudCoorientacion;
import com.academconnect.dto.SolicitudCoorientacionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SolicitudCoorientacionMapper {

    @Mapping(source = "trabajo.id", target = "trabajoId")
    @Mapping(source = "trabajo.titulo", target = "trabajoTitulo")
    @Mapping(source = "invitado.id", target = "invitadoId")
    @Mapping(source = "invitado.nombre", target = "invitadoNombre")
    SolicitudCoorientacionResponse toResponse(SolicitudCoorientacion entity);
}
