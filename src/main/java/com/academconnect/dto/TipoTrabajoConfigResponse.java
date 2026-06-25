package com.academconnect.dto;

import com.academconnect.domain.ModoEvaluacion;
import com.academconnect.domain.TipoTrabajo;
import java.util.List;

public record TipoTrabajoConfigResponse(
        TipoTrabajo tipo,
        ModoEvaluacion modoEvaluacion,
        int evaluadoresDefault,
        List<InstanciaEvaluacionConfigDto> instancias) {
}
