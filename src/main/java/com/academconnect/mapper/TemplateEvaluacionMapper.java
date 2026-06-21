package com.academconnect.mapper;

import com.academconnect.domain.TemplateEvaluacion;
import com.academconnect.dto.TemplateEvaluacionRequest;
import com.academconnect.dto.TemplateEvaluacionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TemplateEvaluacionMapper {

    @Mapping(target = "autorId", source = "autor.id")
    @Mapping(target = "autorNombre", source = "autor.nombre")
    TemplateEvaluacionResponse toResponse(TemplateEvaluacion template);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "autor", ignore = true)
    @Mapping(target = "scope", ignore = true)
    @Mapping(target = "tipoTrabajoAplicable", ignore = true)
    TemplateEvaluacion toEntity(TemplateEvaluacionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "autor", ignore = true)
    @Mapping(target = "scope", ignore = true)
    @Mapping(target = "tipoTrabajoAplicable", ignore = true)
    void update(TemplateEvaluacionRequest request, @MappingTarget TemplateEvaluacion target);
}
