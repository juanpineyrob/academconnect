package com.academconnect.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InstanciaEvaluacionConfigInput(
        @NotBlank @Size(max = 200) String nombre,
        @NotNull @Min(1) Integer evaluadoresRequeridos,
        @NotNull @Min(1) Integer maxIntentos) {
}
