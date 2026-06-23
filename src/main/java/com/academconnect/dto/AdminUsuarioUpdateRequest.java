package com.academconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Edición de datos de un usuario por administrador (sin password ni cambio de rol). */
public record AdminUsuarioUpdateRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 30) String matricula,
        @NotBlank @Size(max = 200) String nombre,
        Integer edad,
        @Size(max = 200) String ubicacion,
        @Min(0) Integer topeAsignaciones,
        @Size(max = 200) String titulacion,
        @Size(max = 200) String cargo,
        @Size(max = 200) String institucion,
        @Size(max = 200) String titulo) {
}
