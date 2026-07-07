package com.academconnect.dto;

import com.academconnect.domain.EstadoInvitacion;
import java.time.Instant;

public record InvitacionOrientacionResponse(
        Long id,
        Long trabajoId,
        String trabajoTitulo,
        Long solicitanteId,
        String solicitanteNombre,
        Long profesorId,
        String profesorNombre,
        EstadoInvitacion estado,
        String motivo,
        String respuesta,
        Instant resueltaEn,
        Instant createdAt) {
}
