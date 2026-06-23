package com.academconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Reseteo de contraseña por administrador. */
public record AdminPasswordResetRequest(
        @NotBlank @Size(min = 8, max = 255) String password) {
}
