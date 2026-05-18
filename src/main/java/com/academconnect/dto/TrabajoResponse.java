package com.academconnect.dto;

import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.TipoTrabajo;
import java.time.Instant;
import java.util.Set;

public record TrabajoResponse(
        Long id,
        String titulo,
        String descripcion,
        TipoTrabajo tipo,
        EstadoTrabajo estado,
        Long orientadorId,
        String orientadorNombre,
        Long estudianteId,
        String estudianteNombre,
        Set<AreaTematicaResponse> areas,
        Instant createdAt,
        Instant updatedAt) {
}
