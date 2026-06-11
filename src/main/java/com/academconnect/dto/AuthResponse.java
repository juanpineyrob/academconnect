package com.academconnect.dto;

import com.academconnect.domain.Rol;

public record AuthResponse(
        String token,
        Long userId,
        String nombre,
        String email,
        Rol rol,
        String fotoUrl) {
}
