package com.academconnect.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import com.academconnect.domain.Asignacion;
import com.academconnect.domain.EstadoAsignacion;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Evaluacion;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.Trabajo;
import com.academconnect.dto.CalificacionCriterioRequest;
import com.academconnect.dto.EvaluacionRequest;
import com.academconnect.dto.EvaluacionResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.factories.AsignacionFactory;
import com.academconnect.factories.TrabajoFactory;
import com.academconnect.factories.UsuarioFactory;
import com.academconnect.mapper.EvaluacionMapper;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.EvaluacionRepository;
import com.academconnect.repository.TrabajoRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EvaluacionServiceTests {

    @InjectMocks
    private EvaluacionService service;

    @Mock
    private EvaluacionRepository evaluacionRepository;

    @Mock
    private AsignacionRepository asignacionRepository;

    @Mock
    private TrabajoRepository trabajoRepository;

    @Mock
    private EvaluacionMapper mapper;

    @Mock
    private ApplicationEventPublisher events;

    private Long existingAsignacionId;
    private Trabajo trabajo;
    private Profesor evaluador;
    private Asignacion asignacionActiva;
    private Asignacion asignacionCancelada;
    private EvaluacionRequest twoCriteriosRequest;

    @BeforeEach
    void setup() {
        existingAsignacionId = 1L;
        evaluador = UsuarioFactory.createProfesor(30L, "evaluador@academconnect.com");
        trabajo = TrabajoFactory.createTrabajo();

        asignacionActiva = AsignacionFactory.createAsignacionActiva(
                existingAsignacionId, trabajo, evaluador, AsignacionFactory.snapshotConUmbral(6.0));

        asignacionCancelada = AsignacionFactory.createAsignacionActiva(
                existingAsignacionId, trabajo, evaluador, AsignacionFactory.snapshotConUmbral(6.0));
        asignacionCancelada.setEstado(EstadoAsignacion.CANCELADA);

        twoCriteriosRequest = new EvaluacionRequest(existingAsignacionId, List.of(
                new CalificacionCriterioRequest("C1", new BigDecimal("8.00"), null, null),
                new CalificacionCriterioRequest("C2", new BigDecimal("5.00"), null, null)),
                null);

        Mockito.when(asignacionRepository.findById(existingAsignacionId))
                .thenReturn(Optional.of(asignacionActiva));
        Mockito.when(evaluacionRepository.findByAsignacionId(existingAsignacionId))
                .thenReturn(Optional.empty());
        Mockito.when(evaluacionRepository.save(Mockito.any()))
                .thenAnswer(i -> i.getArgument(0));
        Mockito.when(mapper.toResponse(Mockito.any())).thenAnswer(inv -> {
            Evaluacion ev = inv.getArgument(0);
            return new EvaluacionResponse(null, existingAsignacionId, null,
                    ev.getCalificacionFinal(), null, List.of(), null, null);
        });
        Mockito.when(asignacionRepository.countByTrabajoIdAndVersionamientoIdAndEstado(
                Mockito.anyLong(), Mockito.anyLong(), Mockito.eq(EstadoAsignacion.ACTIVA)))
                .thenReturn(0L);
        Mockito.when(evaluacionRepository.promedioPorTrabajoYVersion(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(null);
    }

    @Test
    void completarShouldReturnSumPonderadaWhenCriteriosAreValid() {
        EvaluacionResponse response = service.completar(twoCriteriosRequest);

        // 8.0 * 0.6 + 5.0 * 0.4 = 6.80
        Assertions.assertEquals(0, response.calificacionFinal().compareTo(new BigDecimal("6.80")));
    }

    @Test
    void completarShouldThrowBusinessExceptionWhenAsignacionIsNotActive() {
        Mockito.when(asignacionRepository.findById(existingAsignacionId))
                .thenReturn(Optional.of(asignacionCancelada));

        EvaluacionRequest request = new EvaluacionRequest(existingAsignacionId, List.of(), null);

        Throwable ex = Assertions.assertThrows(BusinessException.class,
                () -> service.completar(request));
        Assertions.assertTrue(ex.getMessage().contains("activas"));
    }

    @Test
    void completarShouldThrowBusinessExceptionWhenEvaluacionAlreadyExists() {
        Mockito.when(evaluacionRepository.findByAsignacionId(existingAsignacionId))
                .thenReturn(Optional.of(new Evaluacion()));

        EvaluacionRequest request = new EvaluacionRequest(existingAsignacionId, List.of(), null);

        Throwable ex = Assertions.assertThrows(BusinessException.class,
                () -> service.completar(request));
        Assertions.assertTrue(ex.getMessage().contains("Ya existe"));
    }

    @Test
    void completarShouldThrowBusinessExceptionWhenCriterioIsUnknown() {
        EvaluacionRequest request = new EvaluacionRequest(existingAsignacionId, List.of(
                new CalificacionCriterioRequest("C1", BigDecimal.ONE, null, null),
                new CalificacionCriterioRequest("DESCONOCIDO", BigDecimal.ONE, null, null)),
                null);

        Throwable ex = Assertions.assertThrows(BusinessException.class,
                () -> service.completar(request));
        Assertions.assertTrue(ex.getMessage().contains("Criterio desconocido"));
    }

    @Test
    void completarShouldThrowBusinessExceptionWhenCriterioIsMissing() {
        EvaluacionRequest request = new EvaluacionRequest(existingAsignacionId, List.of(
                new CalificacionCriterioRequest("C1", BigDecimal.ONE, null, null)),
                null);

        Throwable ex = Assertions.assertThrows(BusinessException.class,
                () -> service.completar(request));
        Assertions.assertTrue(ex.getMessage().contains("Falta calificación"));
    }

    @Test
    void completarShouldNotRunAgregadoraWhenActiveAsignacionesRemain() {
        Mockito.when(asignacionRepository.countByTrabajoIdAndVersionamientoIdAndEstado(
                Mockito.anyLong(), Mockito.anyLong(), Mockito.eq(EstadoAsignacion.ACTIVA)))
                .thenReturn(2L);

        service.completar(twoCriteriosRequest);

        Mockito.verify(trabajoRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void completarShouldApproveTrabajoWhenPromedioReachesUmbralExactly() {
        Mockito.when(evaluacionRepository.promedioPorTrabajoYVersion(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(new BigDecimal("6.00"));

        service.completar(twoCriteriosRequest);

        ArgumentCaptor<Trabajo> captor = ArgumentCaptor.forClass(Trabajo.class);
        Mockito.verify(trabajoRepository).save(captor.capture());
        Trabajo saved = captor.getValue();
        Assertions.assertEquals(EstadoTrabajo.APROBADO, saved.getEstado());
        Assertions.assertEquals(0, saved.getPuntajeAgregado().compareTo(new BigDecimal("6.00")));
        Assertions.assertNotNull(saved.getEvaluadoEn());
    }

    @Test
    void completarShouldRejectTrabajoWhenPromedioFallsBelowUmbralByDecimal() {
        Trabajo trabajoConUmbral5 = TrabajoFactory.createTrabajo();
        Asignacion asignacion = AsignacionFactory.createAsignacionActiva(
                existingAsignacionId, trabajoConUmbral5, evaluador, AsignacionFactory.snapshotConUmbral(5.0));
        Mockito.when(asignacionRepository.findById(existingAsignacionId))
                .thenReturn(Optional.of(asignacion));
        Mockito.when(evaluacionRepository.promedioPorTrabajoYVersion(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(new BigDecimal("4.99"));

        service.completar(twoCriteriosRequest);

        ArgumentCaptor<Trabajo> captor = ArgumentCaptor.forClass(Trabajo.class);
        Mockito.verify(trabajoRepository).save(captor.capture());
        Assertions.assertEquals(EstadoTrabajo.RECHAZADO, captor.getValue().getEstado());
        Assertions.assertEquals(0, captor.getValue().getPuntajeAgregado().compareTo(new BigDecimal("4.99")));
    }

    @Test
    void completarShouldAveragePromedioWithoutDissonanceHandling() {
        // E1=9, E2=2 → promedio 5.5 → aprueba con umbral 5
        Trabajo trabajoConUmbral5 = TrabajoFactory.createTrabajo();
        Asignacion asignacion = AsignacionFactory.createAsignacionActiva(
                existingAsignacionId, trabajoConUmbral5, evaluador, AsignacionFactory.snapshotConUmbral(5.0));
        Mockito.when(asignacionRepository.findById(existingAsignacionId))
                .thenReturn(Optional.of(asignacion));
        Mockito.when(evaluacionRepository.promedioPorTrabajoYVersion(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(new BigDecimal("5.50"));

        service.completar(twoCriteriosRequest);

        ArgumentCaptor<Trabajo> captor = ArgumentCaptor.forClass(Trabajo.class);
        Mockito.verify(trabajoRepository).save(captor.capture());
        Assertions.assertEquals(EstadoTrabajo.APROBADO, captor.getValue().getEstado());
    }
}
