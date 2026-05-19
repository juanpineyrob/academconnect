package com.academconnect.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CalificacionCriterioRequest(
        @NotBlank String criterioCodigo,
        @NotNull @DecimalMin("0") BigDecimal puntaje,
        String comentario) {
}
