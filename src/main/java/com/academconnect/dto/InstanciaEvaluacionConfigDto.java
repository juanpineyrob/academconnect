package com.academconnect.dto;

public record InstanciaEvaluacionConfigDto(
        int orden,
        String nombre,
        int evaluadoresRequeridos,
        int maxIntentos) {
}
