package com.academconnect.service;

import com.academconnect.domain.Coorientador;
import com.academconnect.domain.EstadoInvitacion;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.Externo;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.SolicitudCoorientacion;
import com.academconnect.domain.Trabajo;
import com.academconnect.dto.SolicitudCoorientacionRequest;
import com.academconnect.exception.BusinessException;
import com.academconnect.factories.UsuarioFactory;
import com.academconnect.mapper.SolicitudCoorientacionMapper;
import com.academconnect.repository.CoorientadorRepository;
import com.academconnect.repository.SolicitudCoorientacionRepository;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.repository.UsuarioRepository;
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
class SolicitudCoorientacionServiceTests {

    @InjectMocks private SolicitudCoorientacionService service;
    @Mock private SolicitudCoorientacionRepository repository;
    @Mock private TrabajoRepository trabajoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CoorientadorRepository coorientadorRepository;
    @Mock private SolicitudCoorientacionMapper mapper;
    @Mock private ApplicationEventPublisher events;

    private Estudiante estudiante;
    private Profesor orientador;
    private Profesor candidatoProfesor;
    private Externo candidatoExterno;
    private Trabajo trabajo;

    @BeforeEach
    void setup() {
        estudiante = UsuarioFactory.createEstudiante(10L, "alumno@x.uy");
        orientador = UsuarioFactory.createProfesor(20L, "orientador@x.uy");
        candidatoProfesor = UsuarioFactory.createProfesor(30L, "co@x.uy");
        candidatoExterno = UsuarioFactory.createExterno(40L, "externo@x.uy");

        trabajo = new Trabajo();
        trabajo.setId(100L);
        trabajo.setTitulo("Tesis");
        trabajo.setEstado(EstadoTrabajo.EN_DESARROLLO);
        trabajo.setEstudiante(estudiante);
        trabajo.setOrientador(orientador);

        Mockito.when(trabajoRepository.findById(100L)).thenReturn(Optional.of(trabajo));
        Mockito.when(usuarioRepository.findById(30L)).thenReturn(Optional.of(candidatoProfesor));
        Mockito.when(usuarioRepository.findById(40L)).thenReturn(Optional.of(candidatoExterno));
        Mockito.when(coorientadorRepository.countByTrabajoId(100L)).thenReturn(0L);
        Mockito.when(repository.existsByTrabajoIdAndEstado(100L, EstadoInvitacion.PENDIENTE)).thenReturn(false);
        Mockito.when(repository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
    }

    private SolicitudCoorientacionRequest req(Long usuarioId) {
        return new SolicitudCoorientacionRequest(100L, usuarioId, "me gustaría que coorientes");
    }

    @Test
    void crear_okConProfesor() {
        service.crear(req(30L), estudiante.getId());
        ArgumentCaptor<SolicitudCoorientacion> cap = ArgumentCaptor.forClass(SolicitudCoorientacion.class);
        Mockito.verify(repository).save(cap.capture());
        Assertions.assertEquals(EstadoInvitacion.PENDIENTE, cap.getValue().getEstado());
        Assertions.assertEquals(30L, cap.getValue().getInvitado().getId());
    }

    @Test
    void crear_okConExterno() {
        service.crear(req(40L), estudiante.getId());
        Mockito.verify(repository).save(Mockito.any());
    }

    @Test
    void crear_fallaSiNoEsDueno() {
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req(30L), 999L));
    }

    @Test
    void crear_fallaSinOrientador() {
        trabajo.setOrientador(null);
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req(30L), estudiante.getId()));
    }

    @Test
    void crear_fallaSiFinalizado() {
        trabajo.setEstado(EstadoTrabajo.APROBADO);
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req(30L), estudiante.getId()));
    }

    @Test
    void crear_fallaSiYaTieneCoorientador() {
        Mockito.when(coorientadorRepository.countByTrabajoId(100L)).thenReturn(1L);
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req(30L), estudiante.getId()));
    }

    @Test
    void crear_fallaSiYaHayPendiente() {
        Mockito.when(repository.existsByTrabajoIdAndEstado(100L, EstadoInvitacion.PENDIENTE)).thenReturn(true);
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req(30L), estudiante.getId()));
    }

    @Test
    void crear_fallaSiInvitadoEsElOrientador() {
        Mockito.when(usuarioRepository.findById(20L)).thenReturn(Optional.of(orientador));
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req(20L), estudiante.getId()));
    }

    @Test
    void aceptar_creaCoorientadorYNoTocaEstado() {
        SolicitudCoorientacion s = new SolicitudCoorientacion();
        s.setId(7L);
        s.setTrabajo(trabajo);
        s.setInvitado(candidatoProfesor);
        s.setEstado(EstadoInvitacion.PENDIENTE);
        Mockito.when(repository.findById(7L)).thenReturn(Optional.of(s));

        service.aceptar(7L, null, candidatoProfesor.getId());

        Assertions.assertEquals(EstadoInvitacion.ACEPTADA, s.getEstado());
        Assertions.assertEquals(EstadoTrabajo.EN_DESARROLLO, trabajo.getEstado()); // sin cambios
        ArgumentCaptor<Coorientador> cap = ArgumentCaptor.forClass(Coorientador.class);
        Mockito.verify(coorientadorRepository).save(cap.capture());
        Assertions.assertEquals(candidatoProfesor.getId(), cap.getValue().getUsuario().getId());
        Assertions.assertEquals(100L, cap.getValue().getTrabajo().getId());
        Assertions.assertNotNull(cap.getValue().getDesde());
    }

    @Test
    void aceptar_fallaSiNoEsElInvitado() {
        SolicitudCoorientacion s = new SolicitudCoorientacion();
        s.setId(7L);
        s.setTrabajo(trabajo);
        s.setInvitado(candidatoProfesor);
        s.setEstado(EstadoInvitacion.PENDIENTE);
        Mockito.when(repository.findById(7L)).thenReturn(Optional.of(s));
        Assertions.assertThrows(BusinessException.class, () -> service.aceptar(7L, null, 999L));
    }

    @Test
    void rechazar_marcaRechazada() {
        SolicitudCoorientacion s = new SolicitudCoorientacion();
        s.setId(7L);
        s.setTrabajo(trabajo);
        s.setInvitado(candidatoProfesor);
        s.setEstado(EstadoInvitacion.PENDIENTE);
        Mockito.when(repository.findById(7L)).thenReturn(Optional.of(s));
        service.rechazar(7L, null, candidatoProfesor.getId());
        Assertions.assertEquals(EstadoInvitacion.RECHAZADA, s.getEstado());
        Mockito.verify(coorientadorRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void cancelar_soloDueno() {
        SolicitudCoorientacion s = new SolicitudCoorientacion();
        s.setId(7L);
        s.setTrabajo(trabajo);
        s.setInvitado(candidatoProfesor);
        s.setEstado(EstadoInvitacion.PENDIENTE);
        Mockito.when(repository.findById(7L)).thenReturn(Optional.of(s));
        Assertions.assertThrows(BusinessException.class, () -> service.cancelar(7L, 999L));
        service.cancelar(7L, estudiante.getId());
        Assertions.assertEquals(EstadoInvitacion.CANCELADA, s.getEstado());
    }
}
