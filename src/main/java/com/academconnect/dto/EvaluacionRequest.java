package com.academconnect.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record EvaluacionRequest(
        @NotNull Long asignacionId,
        @NotNull @NotEmpty @Valid List<CalificacionCriterioRequest> calificaciones,
        String comentarioGeneral) {
}
