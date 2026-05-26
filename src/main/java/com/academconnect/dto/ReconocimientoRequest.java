package com.academconnect.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReconocimientoRequest(
        @NotBlank @Size(max = 50) String tipo,
        @NotBlank @Size(max = 500) String descripcion,
        @NotNull @Min(1900) @Max(2200) Integer anio) {
}
