package com.academconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academconnect.dto.CargaEvaluadorDto;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.EvaluacionRepository;
import com.academconnect.repository.TrabajoRepository;

@ExtendWith(MockitoExtension.class)
class MetricasServiceTest {

    @Mock TrabajoRepository trabajoRepository;
    @Mock EvaluacionRepository evaluacionRepository;
    @Mock AsignacionRepository asignacionRepository;

    MetricasService service;

    @BeforeEach
    void setUp() {
        service = new MetricasService(trabajoRepository, evaluacionRepository, asignacionRepository);
    }

    @Test
    void gini_cero_cuando_carga_es_igual() {
        when(trabajoRepository.contarPorEstado()).thenReturn(List.of());
        when(evaluacionRepository.promedioTiempoEvaluacionHoras()).thenReturn(null);
        when(asignacionRepository.cargaActivaPorEvaluador()).thenReturn(List.of(
                new CargaEvaluadorDto(1L, "A", 5L),
                new CargaEvaluadorDto(2L, "B", 5L),
                new CargaEvaluadorDto(3L, "C", 5L)));

        var metricas = service.obtenerMetricas();

        assertThat(metricas.giniCarga()).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void gini_maximo_cuando_un_evaluador_concentra_toda_la_carga() {
        when(trabajoRepository.contarPorEstado()).thenReturn(List.of());
        when(evaluacionRepository.promedioTiempoEvaluacionHoras()).thenReturn(null);
        when(asignacionRepository.cargaActivaPorEvaluador()).thenReturn(List.of(
                new CargaEvaluadorDto(1L, "A", 0L),
                new CargaEvaluadorDto(2L, "B", 0L),
                new CargaEvaluadorDto(3L, "C", 10L)));

        var metricas = service.obtenerMetricas();

        // con n=3 y un sólo portador de toda la carga, Gini = 2/3 ≈ 0.667
        assertThat(metricas.giniCarga()).isCloseTo(2.0 / 3.0, within(1e-9));
    }

    @Test
    void gini_cero_cuando_no_hay_evaluadores() {
        when(trabajoRepository.contarPorEstado()).thenReturn(List.of());
        when(evaluacionRepository.promedioTiempoEvaluacionHoras()).thenReturn(null);
        when(asignacionRepository.cargaActivaPorEvaluador()).thenReturn(List.of());

        var metricas = service.obtenerMetricas();

        assertThat(metricas.giniCarga()).isEqualTo(0.0);
    }

    @Test
    void obtenerMetricas_propaga_tiempo_promedio_del_repositorio() {
        when(trabajoRepository.contarPorEstado()).thenReturn(List.of());
        when(evaluacionRepository.promedioTiempoEvaluacionHoras()).thenReturn(24.5);
        when(asignacionRepository.cargaActivaPorEvaluador()).thenReturn(List.of());

        var metricas = service.obtenerMetricas();

        assertThat(metricas.tiempoPromedioEvaluacionHoras()).isEqualTo(24.5);
    }
}
