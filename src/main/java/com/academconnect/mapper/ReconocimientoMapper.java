package com.academconnect.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.academconnect.domain.Reconocimiento;
import com.academconnect.dto.ReconocimientoResponse;

@Mapper(componentModel = "spring")
public interface ReconocimientoMapper {

    @Mapping(source = "otorgadoPor.nombre", target = "otorgadoPorNombre")
    ReconocimientoResponse toResponse(Reconocimiento r);
}
