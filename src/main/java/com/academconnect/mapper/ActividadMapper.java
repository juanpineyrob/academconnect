package com.academconnect.mapper;

import org.mapstruct.Mapper;

import com.academconnect.domain.Actividad;
import com.academconnect.dto.ActividadResponse;

@Mapper(componentModel = "spring")
public interface ActividadMapper {

    ActividadResponse toResponse(Actividad a);
}
