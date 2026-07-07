package com.academconnect.mapper;

import com.academconnect.domain.InvitacionOrientacion;
import com.academconnect.dto.InvitacionOrientacionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvitacionOrientacionMapper {

    @Mapping(source = "trabajo.id", target = "trabajoId")
    @Mapping(source = "trabajo.titulo", target = "trabajoTitulo")
    @Mapping(source = "trabajo.estudiante.id", target = "solicitanteId")
    @Mapping(source = "trabajo.estudiante.nombre", target = "solicitanteNombre")
    @Mapping(source = "profesor.id", target = "profesorId")
    @Mapping(source = "profesor.nombre", target = "profesorNombre")
    InvitacionOrientacionResponse toResponse(InvitacionOrientacion entity);
}
