package com.academconnect.dto;

import com.academconnect.domain.ModoEvaluacion;
import com.academconnect.domain.TipoTrabajo;

public record TipoTrabajoConfigResponse(
        TipoTrabajo tipo,
        ModoEvaluacion modoEvaluacion,
        int evaluadoresDefault) {
}
