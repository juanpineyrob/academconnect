package com.academconnect.dto;

import java.util.List;

public record SugerenciaBancaResponse(
        int evaluadoresRequeridos,
        List<SugerenciaEvaluadorResponse> sugerencias) {
}
