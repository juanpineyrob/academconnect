package com.academconnect.dto;

import java.math.BigDecimal;

import com.academconnect.domain.Rol;

public record SugerenciaEvaluadorResponse(
        Long evaluadorId,
        String nombre,
        String email,
        Rol rol,
        BigDecimal score,
        BigDecimal afinidad,
        BigDecimal cargaNorm,
        BigDecimal disponibilidad) {
}
