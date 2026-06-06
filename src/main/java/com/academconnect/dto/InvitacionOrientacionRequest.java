package com.academconnect.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InvitacionOrientacionRequest(
        @NotNull Long trabajoId,
        @NotNull Long profesorId,
        @Size(max = 1000) String motivo) {
}
