package com.academconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EstablecerPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 255) String password) {
}
