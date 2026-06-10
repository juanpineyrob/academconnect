package com.academconnect.dto;

import java.time.Instant;
import java.util.List;

public record PerfilPublicoResponse(
        Long id,
        String nombre,
        String rol,
        String biografia,
        String ubicacion,
        String fotoUrl,
        String titulacion,
        String cargo,
        String institucion,
        String titulo,
        List<UsuarioAreaTematicaResponse> areas,
        int trabajosPublicados,
        Instant createdAt) {
}
