package com.academconnect.dto;

import com.academconnect.domain.EstadoInvitacion;
import java.time.Instant;

public record SolicitudEvaluacionResponse(
        Long id,
        Long trabajoId,
        String trabajoTitulo,
        Long solicitanteId,
        String solicitanteNombre,
        Long invitadoId,
        String invitadoNombre,
        EstadoInvitacion estado,
        String motivo,
        String respuesta,
        Instant resueltaEn,
        Instant createdAt) {
}
