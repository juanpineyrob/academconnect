package com.academconnect.dto;

import com.academconnect.domain.TemplateScope;
import com.academconnect.domain.TipoTrabajo;
import java.time.Instant;

public record TemplateEvaluacionResponse(
        Long id,
        String nombre,
        String descripcion,
        TemplateScope scope,
        TipoTrabajo tipoTrabajoAplicable,
        String criterios,
        boolean activo,
        Instant createdAt,
        Instant updatedAt) {
}
