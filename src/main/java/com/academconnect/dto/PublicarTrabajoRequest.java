package com.academconnect.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PublicarTrabajoRequest(
        @NotNull @Min(7) @Max(60) Integer duracionDias) {
}
