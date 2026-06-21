package com.academconnect.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.TipoTrabajo;

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
        List<String> keywords,
        List<String> coorientadoresNombres,
        BigDecimal puntajeAgregado,
        Instant evaluadoEn,
        Instant expiraEn,
        boolean oculto,
        String archivoStorageKey,
        Instant createdAt,
        Instant updatedAt) {
}
