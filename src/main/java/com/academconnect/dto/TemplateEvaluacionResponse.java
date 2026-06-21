package com.academconnect.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.academconnect.domain.Visibilidad;

public record TemplateEvaluacionResponse(
        Long id,
        String nombre,
        String descripcion,
        Visibilidad visibilidad,
        Long autorId,
        String autorNombre,
        String criterios,
        boolean activo,
        BigDecimal umbralAprobacion,
        Instant createdAt,
        Instant updatedAt) {
}
