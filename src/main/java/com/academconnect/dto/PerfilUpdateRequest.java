package com.academconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerfilUpdateRequest(
        @NotBlank @Size(max = 200) String nombre,
        Integer edad,
        @Size(max = 200) String ubicacion,
        String biografia,
        @Size(min = 8, max = 255) String password,
        @Size(max = 200) String titulacion,
        @Size(max = 200) String cargo,
        @Size(max = 200) String institucion,
        @Size(max = 200) String titulo) {
}
