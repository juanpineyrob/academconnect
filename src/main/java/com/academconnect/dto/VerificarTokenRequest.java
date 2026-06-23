package com.academconnect.dto;

import jakarta.validation.constraints.NotBlank;

public record VerificarTokenRequest(@NotBlank String token) {
}
