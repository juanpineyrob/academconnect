package com.academconnect.dto;

import java.time.Instant;
import java.util.Set;

public record ProfesorResponse(
        Long id,
        String email,
        String nombre,
        boolean activo,
        Integer edad,
        String ubicacion,
        String biografia,
        String titulacion,
        String cargo,
        Set<UsuarioAreaTematicaResponse> areas,
        Instant createdAt,
        Instant updatedAt) {
}
