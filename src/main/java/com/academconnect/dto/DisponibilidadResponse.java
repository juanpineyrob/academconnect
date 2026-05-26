package com.academconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DisponibilidadResponse(
        Long id,
        LocalDate fecha,
        BigDecimal horasDisponibles) {
}
