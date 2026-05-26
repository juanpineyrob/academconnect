package com.academconnect.mapper;

import org.mapstruct.Mapper;

import com.academconnect.domain.DisponibilidadEvaluador;
import com.academconnect.dto.DisponibilidadResponse;

@Mapper(componentModel = "spring")
public interface DisponibilidadEvaluadorMapper {

    DisponibilidadResponse toResponse(DisponibilidadEvaluador d);
}
