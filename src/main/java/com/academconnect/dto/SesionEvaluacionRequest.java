package com.academconnect.dto;

import java.time.Instant;

import com.academconnect.domain.ModalidadSesion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SesionEvaluacionRequest(
        @NotNull Long trabajoId,
        @NotNull Instant fechaProgramada,
        @NotNull @Min(15) Integer duracionMinutos,
        @Size(max = 255) String ubicacion,
        @NotNull ModalidadSesion modalidad,
        @Size(max = 500) String urlMeet) {
}
