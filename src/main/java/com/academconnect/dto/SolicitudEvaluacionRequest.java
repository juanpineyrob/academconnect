package com.academconnect.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudEvaluacionRequest(
        @NotNull Long trabajoId,
        @NotNull Long usuarioId,
        @Size(max = 1000) String motivo) {
}
