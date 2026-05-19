package com.academconnect.dto;

import com.academconnect.domain.TemplateScope;
import com.academconnect.domain.TipoTrabajo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TemplateEvaluacionRequest(
        @NotBlank @Size(max = 200) String nombre,
        String descripcion,
        @NotNull TemplateScope scope,
        TipoTrabajo tipoTrabajoAplicable,
        @NotBlank String criterios,
        boolean activo) {
}
