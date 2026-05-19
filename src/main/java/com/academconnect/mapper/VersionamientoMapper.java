package com.academconnect.mapper;

import com.academconnect.domain.Versionamiento;
import com.academconnect.dto.VersionamientoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = DocumentoMapper.class)
public interface VersionamientoMapper {

    @Mapping(source = "trabajo.id", target = "trabajoId")
    VersionamientoResponse toResponse(Versionamiento versionamiento);
}
