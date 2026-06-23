package com.academconnect.dto;

import java.time.Instant;

import com.academconnect.domain.EstadoSolicitudCuenta;

public record SolicitudResponse(Long id, String matricula, String email, String nombre,
        EstadoSolicitudCuenta estado, String motivoRechazo, Instant createdAt) {
}
