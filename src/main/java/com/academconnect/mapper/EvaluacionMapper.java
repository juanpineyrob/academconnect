package com.academconnect.mapper;

import com.academconnect.domain.Evaluacion;
import com.academconnect.dto.EvaluacionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CalificacionCriterioMapper.class)
public interface EvaluacionMapper {

    @Mapping(source = "asignacion.id", target = "asignacionId")
    EvaluacionResponse toResponse(Evaluacion evaluacion);
}
