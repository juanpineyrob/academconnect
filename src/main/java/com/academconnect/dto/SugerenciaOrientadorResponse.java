package com.academconnect.dto;

import java.math.BigDecimal;
import java.util.List;

public record SugerenciaOrientadorResponse(
        Long id,
        String nombre,
        String email,
        List<String> areasNombres,
        long cargaActiva,
        BigDecimal afinidad,
        BigDecimal score) {
}
