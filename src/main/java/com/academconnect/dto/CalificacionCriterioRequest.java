package com.academconnect.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CalificacionCriterioRequest(
        @NotBlank String criterioCodigo,
        @NotNull @DecimalMin("0") BigDecimal puntaje,
        String comentario,
        Boolean comentarioPrivado) {
}
