package com.academconnect.dto;

import java.time.Instant;

import com.academconnect.domain.EstadoSesionEvaluacion;
import com.academconnect.domain.ModalidadSesion;

public record SesionEvaluacionResponse(
        Long id,
        Long trabajoId,
        String trabajoTitulo,
        Instant fechaProgramada,
        int duracionMinutos,
        String ubicacion,
        ModalidadSesion modalidad,
        String urlMeet,
        EstadoSesionEvaluacion estado,
        Instant createdAt,
        Instant updatedAt) {
}
