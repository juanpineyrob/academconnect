package com.academconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudCuentaRequest(
        @NotBlank @Size(max = 30) String matricula,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 200) String nombre) {
}
