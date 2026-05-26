package com.academconnect.dto;

import java.math.BigDecimal;

public record StatsEvaluadorResponse(
        long evaluacionesCompletadas,
        BigDecimal tiempoMedioRespuestaDias,
        BigDecimal scoreMedioDado,
        long aprobadosAportados,
        long rechazadosAportados) {
}
