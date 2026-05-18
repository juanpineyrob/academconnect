package com.academconnect.dto;

import java.time.Instant;
import java.util.Set;

public record ExternoResponse(
        Long id,
        String email,
        String nombre,
        boolean activo,
        Integer edad,
        String ubicacion,
        String biografia,
        String institucion,
        String titulo,
        Set<UsuarioAreaTematicaResponse> areas,
        Instant createdAt,
        Instant updatedAt) {
}
