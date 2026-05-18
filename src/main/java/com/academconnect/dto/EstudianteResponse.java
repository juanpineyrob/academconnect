package com.academconnect.dto;

import java.time.Instant;
import java.util.Set;

public record EstudianteResponse(
        Long id,
        String email,
        String nombre,
        boolean activo,
        Integer edad,
        String ubicacion,
        String biografia,
        Set<UsuarioAreaTematicaResponse> areas,
        Instant createdAt,
        Instant updatedAt) {
}
