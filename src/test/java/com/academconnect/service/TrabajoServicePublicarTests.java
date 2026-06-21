package com.academconnect.service;

import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.Trabajo;
import com.academconnect.dto.PublicarTrabajoRequest;
import com.academconnect.dto.TrabajoResponse;
import com.academconnect.event.ActividadEvent;
import com.academconnect.exception.BusinessException;
import com.academconnect.factories.UsuarioFactory;
import com.academconnect.mapper.TrabajoMapper;
import com.academconnect.repository.AreaTematicaRepository;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.EstudianteRepository;
import com.academconnect.repository.ProfesorRepository;
import com.academconnect.repository.SolicitudVinculacionRepository;
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrabajoServicePublicarTests {

    @InjectMocks private TrabajoService service;
    @Mock private TrabajoRepository trabajoRepository;
    @Mock private ProfesorRepository profesorRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AreaTematicaRepository areaTematicaRepository;
    @Mock private AsignacionRepository asignacionRepository;
    @Mock private SolicitudVinculacionRepository solicitudRepository;
    @Mock private TrabajoMapper mapper;
    @Mock private ApplicationEventPublisher events;

    private Profesor profesor;
    private Trabajo borrador;

    @BeforeEach
    void setup() {
        profesor = UsuarioFactory.createProfesor(20L, "profe@x.uy");
        borrador = new Trabajo();
        borrador.setId(100L);
        borrador.setTitulo("Necesidad X");
        borrador.setEstado(EstadoTrabajo.BORRADOR);
        borrador.setTipo(TipoTrabajo.TCC);
        borrador.setOrientador(profesor);
        borrador.setKeywords(List.of("k1","k2","k3"));
        Set<AreaTematica> areas = new HashSet<>();
        AreaTematica a = new AreaTematica();
        a.setId(1L);
        areas.add(a);
        borrador.setAreas(areas);
        Mockito.when(trabajoRepository.findById(100L)).thenReturn(Optional.of(borrador));
        Mockito.when(trabajoRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        Mockito.when(mapper.toResponse(Mockito.any())).thenReturn(null);
    }

    @Test
    void publicarOkTransicionaAAbiertoYSeteaExpiraEn() {
        service.publicar(100L, new PublicarTrabajoRequest(30), 20L);
        Assertions.assertEquals(EstadoTrabajo.ABIERTO, borrador.getEstado());
        Assertions.assertNotNull(borrador.getExpiraEn());
        Assertions.assertTrue(borrador.getExpiraEn().isAfter(java.time.Instant.now().plusSeconds(29 * 86400L)));
    }

    @Test
    void publicarFallaSiNoEsOrientador() {
        Assertions.assertThrows(BusinessException.class,
                () -> service.publicar(100L, new PublicarTrabajoRequest(30), 999L));
    }

    @Test
    void publicarFallaSiNoEstaEnBorrador() {
        borrador.setEstado(EstadoTrabajo.ABIERTO);
        Assertions.assertThrows(BusinessException.class,
                () -> service.publicar(100L, new PublicarTrabajoRequest(30), 20L));
    }

    @Test
    void publicarFallaSinAreas() {
        borrador.setAreas(new HashSet<>());
        Assertions.assertThrows(BusinessException.class,
                () -> service.publicar(100L, new PublicarTrabajoRequest(30), 20L));
    }

    @Test
    void publicarFallaSiDuracionInvalida() {
        Assertions.assertThrows(BusinessException.class,
                () -> service.publicar(100L, new PublicarTrabajoRequest(45), 20L));
    }

    @Test
    void publicarPublicaEventoTrabajoPublicado() {
        service.publicar(100L, new PublicarTrabajoRequest(30), 20L);
        ArgumentCaptor<ActividadEvent> c = ArgumentCaptor.forClass(ActividadEvent.class);
        Mockito.verify(events).publishEvent(c.capture());
        Assertions.assertEquals(TipoActividad.TRABAJO_PUBLICADO, c.getValue().tipo());
    }

    @Test
    void cerrarOkTransicionaACanceladoYAutoRechazaPendientes() {
        borrador.setEstado(EstadoTrabajo.ABIERTO);
        var pendiente = new com.academconnect.domain.SolicitudVinculacion();
        pendiente.setEstado(com.academconnect.domain.EstadoSolicitud.PENDIENTE);
        pendiente.setTrabajo(borrador);
        Mockito.when(solicitudRepository.findByTrabajoIdAndEstado(100L, com.academconnect.domain.EstadoSolicitud.PENDIENTE))
                .thenReturn(List.of(pendiente));

        service.cerrar(100L, 20L);

        Assertions.assertEquals(EstadoTrabajo.CANCELADO, borrador.getEstado());
        Assertions.assertEquals(com.academconnect.domain.EstadoSolicitud.RECHAZADA, pendiente.getEstado());
        Assertions.assertEquals("Trabajo cerrado", pendiente.getRespuesta());
    }

    @Test
    void cerrarFallaSiNoEsOrientador() {
        borrador.setEstado(EstadoTrabajo.ABIERTO);
        Assertions.assertThrows(BusinessException.class, () -> service.cerrar(100L, 999L));
    }

    @Test
    void cerrarFallaSiNoEstaAbierto() {
        borrador.setEstado(EstadoTrabajo.BORRADOR);
        Assertions.assertThrows(BusinessException.class, () -> service.cerrar(100L, 20L));
    }

    // ---- Overrides de administrador: ocultar / mostrar / eliminar ----

    @Test
    void ocultarSeteaOcultoTrueSinCambiarEstado() {
        borrador.setEstado(EstadoTrabajo.APROBADO);
        service.ocultar(100L);
        Assertions.assertTrue(borrador.isOculto());
        Assertions.assertEquals(EstadoTrabajo.APROBADO, borrador.getEstado());
    }

    @Test
    void mostrarSeteaOcultoFalse() {
        borrador.setOculto(true);
        service.mostrar(100L);
        Assertions.assertFalse(borrador.isOculto());
    }

    @Test
    void eliminarBorraAsignacionesYLuegoElTrabajo() {
        var asignacion = new com.academconnect.domain.Asignacion();
        Mockito.when(asignacionRepository.findByTrabajoId(100L)).thenReturn(List.of(asignacion));

        service.eliminar(100L);

        Mockito.verify(asignacionRepository).deleteAllInBatch(List.of(asignacion));
        Mockito.verify(trabajoRepository).delete(borrador);
    }
}
