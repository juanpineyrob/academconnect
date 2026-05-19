package com.academconnect.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.dto.CargaEvaluadorDto;
import com.academconnect.dto.MetricasResponse;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.EvaluacionRepository;
import com.academconnect.repository.TrabajoRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MetricasService {

    private final TrabajoRepository trabajoRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final AsignacionRepository asignacionRepository;

    public MetricasResponse obtenerMetricas() {
        var trabajosPorEstado = trabajoRepository.contarPorEstado();
        var tiempoPromedio = evaluacionRepository.promedioTiempoEvaluacionHoras();
        var cargaPorEvaluador = asignacionRepository.cargaActivaPorEvaluador();
        double gini = calcularGini(cargaPorEvaluador.stream().map(CargaEvaluadorDto::cargaActiva).toList());
        return new MetricasResponse(trabajosPorEstado, tiempoPromedio, cargaPorEvaluador, gini);
    }

    /**
     * Gini coefficient for evaluator load distribution.
     * Returns 0.0 when all evaluators have equal load; approaches 1.0 under extreme concentration.
     */
    private double calcularGini(List<Long> cargas) {
        if (cargas.isEmpty()) return 0.0;
        List<Long> sorted = cargas.stream().sorted().toList();
        long n = sorted.size();
        long sumTotal = sorted.stream().mapToLong(Long::longValue).sum();
        if (sumTotal == 0) return 0.0;
        long weightedSum = 0;
        for (int i = 0; i < sorted.size(); i++) {
            weightedSum += (long)(i + 1) * sorted.get(i);
        }
        return (double)(2 * weightedSum) / ((double) n * sumTotal) - (double)(n + 1) / n;
    }
}
