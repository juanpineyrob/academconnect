package com.academconnect.dto;

import com.academconnect.domain.EstadoSolicitud;
import java.time.Instant;

public record SolicitudVinculacionResponse(
        Long id,
        Long trabajoId,
        String trabajoTitulo,
        Long estudianteId,
        String estudianteNombre,
        EstadoSolicitud estado,
        String motivo,
        String respuesta,
        Instant resueltaEn,
        Instant createdAt) {
}
