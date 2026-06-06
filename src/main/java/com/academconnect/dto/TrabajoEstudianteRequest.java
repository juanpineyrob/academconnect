package com.academconnect.dto;

import com.academconnect.domain.TipoTrabajo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

public record TrabajoEstudianteRequest(
        @NotBlank @Size(max = 300) String titulo,
        String descripcion,
        @NotNull TipoTrabajo tipo,
        Set<Long> areaIds,
        @NotNull @Size(min = 3, max = 8) List<@NotBlank @Size(min = 2, max = 50) String> keywords) {
}
