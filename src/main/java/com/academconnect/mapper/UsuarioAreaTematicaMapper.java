package com.academconnect.mapper;

import com.academconnect.domain.UsuarioAreaTematica;
import com.academconnect.dto.UsuarioAreaTematicaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioAreaTematicaMapper {

    @Mapping(source = "area.id", target = "areaId")
    @Mapping(source = "area.nombre", target = "areaNombre")
    UsuarioAreaTematicaResponse toResponse(UsuarioAreaTematica uata);
}
