package com.academconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        Boolean remember) {

    public boolean rememberOrDefault() {
        return remember != null && remember;
    }
}
