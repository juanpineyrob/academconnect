package com.academconnect.dto;

import java.math.BigDecimal;

public record CalificacionCriterioResponse(
        Long id,
        String criterioCodigo,
        BigDecimal puntaje,
        String comentario,
        boolean comentarioPrivado) {
}
