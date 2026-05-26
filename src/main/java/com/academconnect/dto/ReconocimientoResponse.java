package com.academconnect.dto;

import java.time.Instant;

public record ReconocimientoResponse(
        Long id,
        String tipo,
        String descripcion,
        int anio,
        String otorgadoPorNombre,
        Instant createdAt) {
}
