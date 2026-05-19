package com.academconnect.dto;

import java.time.Instant;
import java.util.Set;

import com.academconnect.domain.Rol;

public record PerfilResponse(
        Long id,
        String email,
        String nombre,
        boolean activo,
        Rol rol,
        Integer edad,
        String ubicacion,
        String biografia,
        String titulacion,
        String cargo,
        String institucion,
        String titulo,
        Set<UsuarioAreaTematicaResponse> areas,
        Instant createdAt,
        Instant updatedAt) {
}
