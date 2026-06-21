package com.academconnect.dto;

import java.math.BigDecimal;

import com.academconnect.domain.Visibilidad;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TemplateEvaluacionRequest(
        @NotBlank @Size(max = 200) String nombre,
        String descripcion,
        @NotNull Visibilidad visibilidad,
        @NotBlank String criterios,
        boolean activo,
        @NotNull @DecimalMin("0.0") BigDecimal umbralAprobacion) {
}
