package com.academconnect.dto;

import com.academconnect.domain.ModoEvaluacion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TipoTrabajoConfigRequest(
        @NotNull ModoEvaluacion modoEvaluacion,
        @NotNull @Min(1) Integer evaluadoresDefault) {
}
