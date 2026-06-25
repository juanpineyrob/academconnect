package com.academconnect.dto;

import java.math.BigDecimal;

public record InstanciaEvaluacionDto(
        Long id,
        String nombre,
        int orden,
        int intento,
        String estado,
        BigDecimal puntajeAgregado) {}
