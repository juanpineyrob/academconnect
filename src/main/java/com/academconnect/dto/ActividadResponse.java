package com.academconnect.dto;

import java.time.Instant;

import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.VisibilidadActividad;

public record ActividadResponse(
        Long id,
        TipoActividad tipo,
        Long actorId,
        String recursoTipo,
        Long recursoId,
        String payload,
        VisibilidadActividad visibilidad,
        Instant createdAt) {
}
