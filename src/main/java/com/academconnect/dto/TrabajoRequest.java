package com.academconnect.dto;

import com.academconnect.domain.TipoTrabajo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record TrabajoRequest(
        @NotBlank @Size(max = 300) String titulo,
        String descripcion,
        @NotNull TipoTrabajo tipo,
        @NotNull Long orientadorId,
        Set<Long> areaIds) {
}
