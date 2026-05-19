package com.academconnect.mapper;

import com.academconnect.domain.CalificacionCriterio;
import com.academconnect.dto.CalificacionCriterioResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CalificacionCriterioMapper {

    CalificacionCriterioResponse toResponse(CalificacionCriterio calificacion);
}
