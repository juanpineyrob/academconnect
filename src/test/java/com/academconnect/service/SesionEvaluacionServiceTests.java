package com.academconnect.service;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.academconnect.domain.Asignacion;
import com.academconnect.domain.EstadoSesionEvaluacion;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.ModalidadSesion;
import com.academconnect.domain.ModoEvaluacion;
import com.academconnect.domain.SesionEvaluacion;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.Trabajo;
import com.academconnect.dto.SesionEvaluacionRequest;
import com.academconnect.exception.BusinessException;
import com.academconnect.factories.TipoTrabajoConfigFactory;
import com.academconnect.factories.TrabajoFactory;
import com.academconnect.mapper.SesionEvaluacionMapper;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.SesionEvaluacionRepository;
import com.academconnect.repository.TipoTrabajoConfigRepository;
import com.academconnect.repository.TrabajoRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SesionEvaluacionServiceTests {

    @InjectMocks
    private SesionEvaluacionService service;

    @Mock
    private SesionEvaluacionRepository repository;

    @Mock
    private TrabajoRepository trabajoRepository;

    @Mock
    private TipoTrabajoConfigRepository tipoConfigRepository;

    @Mock
    private AsignacionRepository asignacionRepository;

    @Mock
    private ApplicationEventPublisher events;

    @Mock
    private SesionEvaluacionMapper mapper;

    private Long trabajoSincId, trabajoAsincId, sesionId, asignacionId, otraAsignacionId;
    private Trabajo trabajoSinc, trabajoAsinc, otroTrabajo;
    private SesionEvaluacion sesionProgramada;
    private SesionEvaluacion sesionFinalizada;
    private Asignacion asignacionMismoTrabajo;
    private Asignacion asignacionOtroTrabajo;

    @BeforeEach
    void setup() {
        trabajoSincId = 1L;
        trabajoAsincId = 2L;
        sesionId = 100L;
        asignacionId = 50L;
        otraAsignacionId = 51L;

        trabajoSinc = TrabajoFactory.createTrabajo(trabajoSincId, TipoTrabajo.TCC, EstadoTrabajo.EN_EVALUACION);
        trabajoAsinc = TrabajoFactory.createTrabajo(trabajoAsincId, TipoTrabajo.PAPER, EstadoTrabajo.EN_EVALUACION);
        otroTrabajo = TrabajoFactory.createTrabajo(3L, TipoTrabajo.TCC, EstadoTrabajo.EN_EVALUACION);

        sesionProgramada = new SesionEvaluacion();
        sesionProgramada.setTrabajo(trabajoSinc);
        sesionProgramada.setEstado(EstadoSesionEvaluacion.PROGRAMADA);
        ReflectionTestUtils.setField(sesionProgramada, "id", sesionId);

        sesionFinalizada = new SesionEvaluacion();
        sesionFinalizada.setTrabajo(trabajoSinc);
        sesionFinalizada.setEstado(EstadoSesionEvaluacion.FINALIZADA);
        ReflectionTestUtils.setField(sesionFinalizada, "id", 101L);

        asignacionMismoTrabajo = new Asignacion();
        asignacionMismoTrabajo.setTrabajo(trabajoSinc);
        ReflectionTestUtils.setField(asignacionMismoTrabajo, "id", asignacionId);

        asignacionOtroTrabajo = new Asignacion();
        asignacionOtroTrabajo.setTrabajo(otroTrabajo);
        ReflectionTestUtils.setField(asignacionOtroTrabajo, "id", otraAsignacionId);

        Mockito.when(trabajoRepository.findById(trabajoSincId)).thenReturn(Optional.of(trabajoSinc));
        Mockito.when(trabajoRepository.findById(trabajoAsincId)).thenReturn(Optional.of(trabajoAsinc));
        Mockito.when(tipoConfigRepository.findById(TipoTrabajo.TCC))
                .thenReturn(Optional.of(TipoTrabajoConfigFactory.createConfig(TipoTrabajo.TCC, ModoEvaluacion.SINCRONO)));
        Mockito.when(tipoConfigRepository.findById(TipoTrabajo.PAPER))
                .thenReturn(Optional.of(TipoTrabajoConfigFactory.createConfig(TipoTrabajo.PAPER, ModoEvaluacion.ASINCRONO)));
        Mockito.when(repository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        Mockito.when(repository.findById(sesionId)).thenReturn(Optional.of(sesionProgramada));
        Mockito.when(repository.findById(101L)).thenReturn(Optional.of(sesionFinalizada));
        Mockito.when(asignacionRepository.findById(asignacionId)).thenReturn(Optional.of(asignacionMismoTrabajo));
        Mockito.when(asignacionRepository.findById(otraAsignacionId)).thenReturn(Optional.of(asignacionOtroTrabajo));
    }

    @Test
    void crearShouldThrowBusinessExceptionWhenTipoIsAsincrono() {
        SesionEvaluacionRequest request = new SesionEvaluacionRequest(
                trabajoAsincId, Instant.now().plusSeconds(3600), 60, "Sala A",
                ModalidadSesion.PRESENCIAL, null);

        Throwable ex = Assertions.assertThrows(BusinessException.class,
                () -> service.crear(request));
        Assertions.assertTrue(ex.getMessage().contains("no admite sesiones"));
    }

    @Test
    void crearShouldDoNothingWhenTipoIsSincrono() {
        SesionEvaluacionRequest request = new SesionEvaluacionRequest(
                trabajoSincId, Instant.now().plusSeconds(3600), 60, "Sala A",
                ModalidadSesion.PRESENCIAL, null);

        Assertions.assertDoesNotThrow(() -> service.crear(request));
    }

    @Test
    void crearShouldThrowBusinessExceptionWhenFechaIsInThePast() {
        SesionEvaluacionRequest request = new SesionEvaluacionRequest(
                trabajoSincId, Instant.now().minusSeconds(3600), 60, "Sala A",
                ModalidadSesion.PRESENCIAL, null);

        Throwable ex = Assertions.assertThrows(BusinessException.class,
                () -> service.crear(request));
        Assertions.assertTrue(ex.getMessage().contains("futura"));
    }

    @Test
    void vincularAsignacionShouldThrowBusinessExceptionWhenAsignacionBelongsToAnotherTrabajo() {
        Throwable ex = Assertions.assertThrows(BusinessException.class,
                () -> service.vincularAsignacion(sesionId, otraAsignacionId));
        Assertions.assertTrue(ex.getMessage().contains("otro trabajo"));
    }

    @Test
    void vincularAsignacionShouldThrowBusinessExceptionWhenSesionIsFinalizada() {
        Throwable ex = Assertions.assertThrows(BusinessException.class,
                () -> service.vincularAsignacion(101L, asignacionId));
        Assertions.assertTrue(ex.getMessage().contains("finalizada"));
    }
}
