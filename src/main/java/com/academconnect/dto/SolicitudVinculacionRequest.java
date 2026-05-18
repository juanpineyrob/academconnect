package com.academconnect.dto;

import jakarta.validation.constraints.NotNull;

public record SolicitudVinculacionRequest(
        @NotNull Long trabajoId,
        @NotNull Long estudianteId,
        String motivo) {
}
