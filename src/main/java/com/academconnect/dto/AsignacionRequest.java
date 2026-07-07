package com.academconnect.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record AsignacionRequest(
        @NotNull Long trabajoId,
        @NotNull Long versionamientoId,
        @NotNull Long evaluadorId,
        // Opcional: si es null, la asignación nace sin rúbrica y el evaluador la elige al entrar.
        Long templateEvaluacionId,
        Instant vencimientoEn) {
}
