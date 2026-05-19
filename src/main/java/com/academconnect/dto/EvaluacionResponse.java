package com.academconnect.dto;

import com.academconnect.domain.EstadoEvaluacion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record EvaluacionResponse(
        Long id,
        Long asignacionId,
        EstadoEvaluacion estado,
        BigDecimal calificacionFinal,
        String comentarioGeneral,
        List<CalificacionCriterioResponse> calificaciones,
        Instant completadaEn,
        Instant createdAt) {
}
