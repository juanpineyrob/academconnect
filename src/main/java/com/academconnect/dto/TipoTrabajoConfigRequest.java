package com.academconnect.dto;

import com.academconnect.domain.ModoEvaluacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TipoTrabajoConfigRequest(
        @NotNull ModoEvaluacion modoEvaluacion,
        @NotNull @Min(1) Integer evaluadoresDefault,
        boolean secuencial,
        @Valid List<InstanciaEvaluacionConfigInput> instancias) {
}
