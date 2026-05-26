package com.academconnect.dto;

import java.math.BigDecimal;

public record CargaEvaluadorResponse(
        long activas,
        int tope,
        double porcentaje,
        BigDecimal disponibleSemanal) {
}
