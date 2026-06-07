package com.academconnect.service;

import com.academconnect.domain.EstadoInvitacion;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.InvitacionOrientacion;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.Trabajo;
import com.academconnect.dto.InvitacionOrientacionRequest;
import com.academconnect.dto.InvitacionOrientacionResponse;
import com.academconnect.event.ActividadEvent;
import com.academconnect.exception.BusinessException;
import com.academconnect.factories.UsuarioFactory;
import com.academconnect.mapper.InvitacionOrientacionMapper;
import com.academconnect.repository.InvitacionOrientacionRepository;
import com.academconnect.repository.ProfesorRepository;
import com.academconnect.repository.TrabajoRepository;
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

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvitacionOrientacionServiceTests {

    @InjectMocks private InvitacionOrientacionService service;
    @Mock private InvitacionOrientacionRepository repository;
    @Mock private TrabajoRepository trabajoRepository;
    @Mock private ProfesorRepository profesorRepository;
    @Mock private InvitacionOrientacionMapper mapper;
    @Mock private ApplicationEventPublisher events;

    private Estudiante estudiante;
    private Profesor profesor;
    private Trabajo trabajoBorrador;

    @BeforeEach
    void setup() {
        estudiante = UsuarioFactory.createEstudiante(10L, "alumno@x.uy");
        profesor = UsuarioFactory.createProfesor(20L, "profe@x.uy");
        trabajoBorrador = new Trabajo();
        trabajoBorrador.setId(100L);
        trabajoBorrador.setTitulo("Borrador");
        trabajoBorrador.setEstado(EstadoTrabajo.BORRADOR);
        trabajoBorrador.setEstudiante(estudiante);

        Mockito.when(trabajoRepository.findById(100L)).thenReturn(Optional.of(trabajoBorrador));
        Mockito.when(profesorRepository.findById(20L)).thenReturn(Optional.of(profesor));
        Mockito.when(repository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        Mockito.when(mapper.toResponse(Mockito.any())).thenReturn(null);
    }

    @Test
    void crearOkCreaInvitacionEnPendiente() {
        var req = new InvitacionOrientacionRequest(100L, 20L, "Quisiera tu orientación");
        service.crear(req, 10L);

        ArgumentCaptor<InvitacionOrientacion> captor = ArgumentCaptor.forClass(InvitacionOrientacion.class);
        Mockito.verify(repository).save(captor.capture());
        InvitacionOrientacion i = captor.getValue();
        Assertions.assertEquals(EstadoInvitacion.PENDIENTE, i.getEstado());
        Assertions.assertEquals(trabajoBorrador, i.getTrabajo());
        Assertions.assertEquals(profesor, i.getProfesor());
        Assertions.assertEquals("Quisiera tu orientación", i.getMotivo());
    }

    @Test
    void crearFallaSiUsuarioNoEsDuenio() {
        var req = new InvitacionOrientacionRequest(100L, 20L, "x");
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req, 999L));
    }

    @Test
    void crearFallaSiTrabajoNoEstaEnBorrador() {
        trabajoBorrador.setEstado(EstadoTrabajo.EN_DESARROLLO);
        var req = new InvitacionOrientacionRequest(100L, 20L, "x");
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req, 10L));
    }

    @Test
    void crearFallaSiYaTieneOrientador() {
        trabajoBorrador.setOrientador(profesor);
        var req = new InvitacionOrientacionRequest(100L, 20L, "x");
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req, 10L));
    }

    @Test
    void crearFallaSiYaHayPendiente() {
        Mockito.when(repository.existsByTrabajoIdAndEstado(100L, EstadoInvitacion.PENDIENTE))
                .thenReturn(true);
        var req = new InvitacionOrientacionRequest(100L, 20L, "x");
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req, 10L));
    }

    @Test
    void crearPublicaEventoEnviado() {
        var req = new InvitacionOrientacionRequest(100L, 20L, "x");
        service.crear(req, 10L);
        Mockito.verify(events).publishEvent(Mockito.any(ActividadEvent.class));
    }

    @Test
    void aceptarOkVinculaOrientadorYTransicionaTrabajo() {
        InvitacionOrientacion i = new InvitacionOrientacion();
        i.setId(500L);
        i.setTrabajo(trabajoBorrador);
        i.setProfesor(profesor);
        i.setEstado(EstadoInvitacion.PENDIENTE);
        Mockito.when(repository.findById(500L)).thenReturn(Optional.of(i));

        service.aceptar(500L, new com.academconnect.dto.RespuestaInvitacionRequest("OK, acepto"), 20L);

        Assertions.assertEquals(EstadoInvitacion.ACEPTADA, i.getEstado());
        Assertions.assertEquals("OK, acepto", i.getRespuesta());
        Assertions.assertNotNull(i.getResueltaEn());
        Assertions.assertEquals(profesor, trabajoBorrador.getOrientador());
        Assertions.assertEquals(EstadoTrabajo.EN_DESARROLLO, trabajoBorrador.getEstado());
    }

    @Test
    void aceptarFallaSiProfesorNoEsElInvitado() {
        InvitacionOrientacion i = new InvitacionOrientacion();
        i.setId(501L);
        i.setTrabajo(trabajoBorrador);
        i.setProfesor(profesor);
        i.setEstado(EstadoInvitacion.PENDIENTE);
        Mockito.when(repository.findById(501L)).thenReturn(Optional.of(i));

        Assertions.assertThrows(BusinessException.class,
                () -> service.aceptar(501L, null, 999L));
    }

    @Test
    void aceptarFallaSiNoEstaPendiente() {
        InvitacionOrientacion i = new InvitacionOrientacion();
        i.setId(502L);
        i.setTrabajo(trabajoBorrador);
        i.setProfesor(profesor);
        i.setEstado(EstadoInvitacion.RECHAZADA);
        Mockito.when(repository.findById(502L)).thenReturn(Optional.of(i));

        Assertions.assertThrows(BusinessException.class,
                () -> service.aceptar(502L, null, 20L));
    }
}
