package com.academconnect.dto;

import com.academconnect.domain.Rol;

/** Vista administrativa de un usuario (campos comunes + específicos de subtipo cuando aplican). */
public record AdminUsuarioResponse(
        Long id,
        String email,
        String matricula,
        String nombre,
        Rol rol,
        boolean activo,
        Integer edad,
        String ubicacion,
        int topeAsignaciones,
        String titulacion,
        String cargo,
        String institucion,
        String titulo) {
}
