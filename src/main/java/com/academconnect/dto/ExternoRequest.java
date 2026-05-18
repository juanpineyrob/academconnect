package com.academconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExternoRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 255) String password,
        @NotBlank @Size(max = 200) String nombre,
        Integer edad,
        @Size(max = 200) String ubicacion,
        String biografia,
        @NotBlank @Size(max = 200) String institucion,
        @NotBlank @Size(max = 200) String titulo) {
}
