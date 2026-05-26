package com.academconnect.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.academconnect.domain.EstadoAsignacion;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Profesor;
import com.academconnect.dto.CargaEvaluadorDto;
import com.academconnect.dto.CargaEvaluadorResponse;
import com.academconnect.dto.MetricasResponse;
import com.academconnect.dto.StatsEvaluadorResponse;
import com.academconnect.dto.StatsPublicasResponse;
import com.academconnect.factories.UsuarioFactory;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.DisponibilidadEvaluadorRepository;
import com.academconnect.repository.EvaluacionRepository;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MetricasServiceTests {

    @InjectMocks
    private MetricasService service;

    @Mock
    private TrabajoRepository trabajoRepository;

    @Mock
    private EvaluacionRepository evaluacionRepository;

    @Mock
    private AsignacionRepository asignacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private DisponibilidadEvaluadorRepository disponibilidadRepository;

    private String evaluadorEmail;
    private Profesor evaluador;

    @BeforeEach
    void setup() {
        evaluadorEmail = "evaluador@academconnect.com";
        evaluador = UsuarioFactory.createProfesor(10L, evaluadorEmail);
        evaluador.setTopeAsignaciones(10);

        Mockito.when(trabajoRepository.contarPorEstado()).thenReturn(List.of());
        Mockito.when(evaluacionRepository.promedioTiempoEvaluacionHoras()).thenReturn(null);
        Mockito.when(asignacionRepository.cargaActivaPorEvaluador()).thenReturn(List.of());
        Mockito.when(usuarioRepository.findByEmail(evaluadorEmail)).thenReturn(Optional.of(evaluador));
    }

    @Test
    void obtenerMetricasShouldReturnGiniZeroWhenCargaIsEqual() {
        Mockito.when(asignacionRepository.cargaActivaPorEvaluador()).thenReturn(List.of(
                new CargaEvaluadorDto(1L, "A", 5L),
                new CargaEvaluadorDto(2L, "B", 5L),
                new CargaEvaluadorDto(3L, "C", 5L)));

        MetricasResponse metricas = service.obtenerMetricas();

        Assertions.assertEquals(0.0, metricas.giniCarga(), 1e-9);
    }

    @Test
    void obtenerMetricasShouldReturnMaxGiniWhenOneEvaluadorConcentratesAllLoad() {
        Mockito.when(asignacionRepository.cargaActivaPorEvaluador()).thenReturn(List.of(
                new CargaEvaluadorDto(1L, "A", 0L),
                new CargaEvaluadorDto(2L, "B", 0L),
                new CargaEvaluadorDto(3L, "C", 10L)));

        MetricasResponse metricas = service.obtenerMetricas();

        // con n=3 y un único portador de toda la carga, Gini = 2/3 ≈ 0.667
        Assertions.assertEquals(2.0 / 3.0, metricas.giniCarga(), 1e-9);
    }

    @Test
    void obtenerMetricasShouldReturnGiniZeroWhenThereAreNoEvaluadores() {
        MetricasResponse metricas = service.obtenerMetricas();

        Assertions.assertEquals(0.0, metricas.giniCarga());
    }

    @Test
    void obtenerMetricasShouldPropagateTiempoPromedioFromRepository() {
        Mockito.when(evaluacionRepository.promedioTiempoEvaluacionHoras()).thenReturn(24.5);

        MetricasResponse metricas = service.obtenerMetricas();

        Assertions.assertEquals(24.5, metricas.tiempoPromedioEvaluacionHoras());
    }

    @Test
    void statsPublicasShouldCountAprobadosAndAreasAndEvaluadores() {
        Mockito.when(trabajoRepository.countByEstado(EstadoTrabajo.APROBADO)).thenReturn(42L);
        Mockito.when(trabajoRepository.countAreasDistintasConTrabajosEnEstados(
                List.of(EstadoTrabajo.APROBADO))).thenReturn(7L);
        Mockito.when(usuarioRepository.contarEvaluadoresActivos()).thenReturn(15L);

        StatsPublicasResponse stats = service.statsPublicas();

        Assertions.assertEquals(42L, stats.trabajosPublicados());
        Assertions.assertEquals(7L, stats.areasActivas());
        Assertions.assertEquals(15L, stats.evaluadoresActivos());
    }

    @Test
    void cargaEvaluadorShouldComputePorcentajeOverTope() {
        Mockito.when(asignacionRepository.countByEvaluadorIdAndEstado(
                evaluador.getId(), EstadoAsignacion.ACTIVA)).thenReturn(3L);
        Mockito.when(disponibilidadRepository.totalHoras(
                Mockito.eq(evaluador.getId()), Mockito.any(), Mockito.any()))
                .thenReturn(new BigDecimal("20.00"));

        CargaEvaluadorResponse carga = service.cargaEvaluador(evaluadorEmail);

        Assertions.assertEquals(3L, carga.activas());
        Assertions.assertEquals(10, carga.tope());
        Assertions.assertEquals(30.0, carga.porcentaje());
        Assertions.assertEquals(0, carga.disponibleSemanal().compareTo(new BigDecimal("20.00")));
    }

    @Test
    void statsEvaluadorShouldPropagateMetricasFromRepository() {
        Mockito.when(evaluacionRepository.countCompletadasPorEvaluador(evaluador.getId())).thenReturn(12L);
        Mockito.when(evaluacionRepository.tiempoMedioHorasPorEvaluador(evaluador.getId())).thenReturn(48.0);
        Mockito.when(evaluacionRepository.scoreMedioPorEvaluador(evaluador.getId()))
                .thenReturn(new BigDecimal("7.80"));
        Mockito.when(trabajoRepository.countAprobadosConEvaluadoresIncluyendo(evaluador.getId())).thenReturn(8L);
        Mockito.when(trabajoRepository.countRechazadosConEvaluadoresIncluyendo(evaluador.getId())).thenReturn(2L);

        StatsEvaluadorResponse stats = service.statsEvaluador(evaluadorEmail);

        Assertions.assertEquals(12L, stats.evaluacionesCompletadas());
        Assertions.assertEquals(0, stats.tiempoMedioRespuestaDias().compareTo(new BigDecimal("2.00")));
        Assertions.assertEquals(0, stats.scoreMedioDado().compareTo(new BigDecimal("7.80")));
        Assertions.assertEquals(8L, stats.aprobadosAportados());
        Assertions.assertEquals(2L, stats.rechazadosAportados());
    }
}
