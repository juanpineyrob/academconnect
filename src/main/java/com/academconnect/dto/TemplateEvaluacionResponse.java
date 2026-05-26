package com.academconnect.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.academconnect.domain.TemplateScope;
import com.academconnect.domain.TipoTrabajo;

public record TemplateEvaluacionResponse(
        Long id,
        String nombre,
        String descripcion,
        TemplateScope scope,
        TipoTrabajo tipoTrabajoAplicable,
        String criterios,
        boolean activo,
        BigDecimal umbralAprobacion,
        Instant createdAt,
        Instant updatedAt) {
}
