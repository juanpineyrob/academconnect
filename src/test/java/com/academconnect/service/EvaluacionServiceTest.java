package com.academconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academconnect.domain.Asignacion;
import com.academconnect.domain.EstadoAsignacion;
import com.academconnect.domain.Evaluacion;
import com.academconnect.domain.Trabajo;
import com.academconnect.dto.CalificacionCriterioRequest;
import com.academconnect.dto.EvaluacionRequest;
import com.academconnect.dto.EvaluacionResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.mapper.EvaluacionMapper;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.EvaluacionRepository;
import com.academconnect.repository.TrabajoRepository;

@ExtendWith(MockitoExtension.class)
class EvaluacionServiceTest {

    private static final String SNAPSHOT_2_CRITERIOS =
            """
            [
              {"codigo":"C1","nombre":"Claridad","peso":0.6,"escalaMin":0,"escalaMax":10},
              {"codigo":"C2","nombre":"Originalidad","peso":0.4,"escalaMin":0,"escalaMax":10}
            ]
            """;

    @Mock EvaluacionRepository evaluacionRepository;
    @Mock AsignacionRepository asignacionRepository;
    @Mock TrabajoRepository trabajoRepository;
    @Mock EvaluacionMapper mapper;

    EvaluacionService service;

    @BeforeEach
    void setUp() {
        service = new EvaluacionService(evaluacionRepository, asignacionRepository, trabajoRepository, mapper);
    }

    @Test
    void completar_calcula_suma_ponderada_correctamente() {
        var asignacion = asignacionActiva(SNAPSHOT_2_CRITERIOS);
        when(asignacionRepository.findById(1L)).thenReturn(Optional.of(asignacion));
        when(evaluacionRepository.findByAsignacionId(1L)).thenReturn(Optional.empty());
        when(evaluacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenAnswer(inv -> {
            Evaluacion ev = inv.getArgument(0);
            return new EvaluacionResponse(null, 1L, null, ev.getCalificacionFinal(), null, List.of(), null, null);
        });

        var request = new EvaluacionRequest(1L, List.of(
                new CalificacionCriterioRequest("C1", new BigDecimal("8.00"), null),
                new CalificacionCriterioRequest("C2", new BigDecimal("5.00"), null)),
                null);

        var response = service.completar(request);

        // 8.0 * 0.6 + 5.0 * 0.4 = 4.8 + 2.0 = 6.8
        assertThat(response.calificacionFinal()).isEqualByComparingTo("6.80");
    }

    @Test
    void completar_lanza_excepcion_si_asignacion_no_activa() {
        var asignacion = asignacionActiva(SNAPSHOT_2_CRITERIOS);
        asignacion.setEstado(EstadoAsignacion.CANCELADA);
        when(asignacionRepository.findById(1L)).thenReturn(Optional.of(asignacion));

        var request = new EvaluacionRequest(1L, List.of(), null);

        assertThatThrownBy(() -> service.completar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("activas");
    }

    @Test
    void completar_lanza_excepcion_si_ya_existe_evaluacion() {
        var asignacion = asignacionActiva(SNAPSHOT_2_CRITERIOS);
        when(asignacionRepository.findById(1L)).thenReturn(Optional.of(asignacion));
        when(evaluacionRepository.findByAsignacionId(1L)).thenReturn(Optional.of(new Evaluacion()));

        var request = new EvaluacionRequest(1L, List.of(), null);

        assertThatThrownBy(() -> service.completar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    void completar_lanza_excepcion_si_criterio_desconocido() {
        var asignacion = asignacionActiva(SNAPSHOT_2_CRITERIOS);
        when(asignacionRepository.findById(1L)).thenReturn(Optional.of(asignacion));
        when(evaluacionRepository.findByAsignacionId(1L)).thenReturn(Optional.empty());

        var request = new EvaluacionRequest(1L, List.of(
                new CalificacionCriterioRequest("C1", BigDecimal.ONE, null),
                new CalificacionCriterioRequest("DESCONOCIDO", BigDecimal.ONE, null)),
                null);

        assertThatThrownBy(() -> service.completar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Criterio desconocido");
    }

    @Test
    void completar_lanza_excepcion_si_falta_criterio() {
        var asignacion = asignacionActiva(SNAPSHOT_2_CRITERIOS);
        when(asignacionRepository.findById(1L)).thenReturn(Optional.of(asignacion));
        when(evaluacionRepository.findByAsignacionId(1L)).thenReturn(Optional.empty());

        var request = new EvaluacionRequest(1L, List.of(
                new CalificacionCriterioRequest("C1", BigDecimal.ONE, null)),
                null);

        assertThatThrownBy(() -> service.completar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Falta calificación");
    }

    private static Asignacion asignacionActiva(String snapshot) {
        var trabajo = new Trabajo();
        var asignacion = new Asignacion();
        asignacion.setTrabajo(trabajo);
        asignacion.setEstado(EstadoAsignacion.ACTIVA);
        asignacion.setTemplateSnapshot(snapshot);
        asignacion.setAsignadaEn(Instant.now());
        org.springframework.test.util.ReflectionTestUtils.setField(asignacion, "id", 1L);
        return asignacion;
    }
}
