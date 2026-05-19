package com.academconnect.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record AsignacionRequest(
        @NotNull Long trabajoId,
        @NotNull Long versionamientoId,
        @NotNull Long evaluadorId,
        @NotNull Long templateEvaluacionId,
        Instant vencimientoEn) {
}
