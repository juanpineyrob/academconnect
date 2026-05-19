package com.academconnect.dto;

import java.util.List;

public record MetricasResponse(
        List<TrabajosPorEstadoDto> trabajosPorEstado,
        Double tiempoPromedioEvaluacionHoras,
        List<CargaEvaluadorDto> cargaPorEvaluador,
        Double giniCarga) {
}
