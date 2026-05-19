package com.academconnect.dto;

import com.academconnect.domain.EstadoAsignacion;
import java.time.Instant;

public record AsignacionResponse(
        Long id,
        Long trabajoId,
        String trabajoTitulo,
        Long versionamientoId,
        int versionNumero,
        Long evaluadorId,
        String evaluadorNombre,
        String templateSnapshot,
        Instant asignadaEn,
        Instant vencimientoEn,
        EstadoAsignacion estado,
        Instant createdAt) {
}
