package com.academconnect.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.academconnect.domain.SesionEvaluacion;
import com.academconnect.dto.SesionEvaluacionResponse;

@Mapper(componentModel = "spring")
public interface SesionEvaluacionMapper {

    @Mapping(source = "trabajo.id", target = "trabajoId")
    @Mapping(source = "trabajo.titulo", target = "trabajoTitulo")
    SesionEvaluacionResponse toResponse(SesionEvaluacion s);
}
