package com.academconnect.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.academconnect.AbstractJpaTest;
import com.academconnect.domain.EstadoSolicitud;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.SolicitudVinculacion;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.Trabajo;

class SolicitudVinculacionConstraintsTest extends AbstractJpaTest {

    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private TrabajoRepository trabajoRepository;
    @Autowired private SolicitudVinculacionRepository solicitudRepository;

    @Test
    void solo_una_solicitud_pendiente_por_par_trabajo_estudiante() {
        Estudiante est = newEstudiante("s@example.com");
        Profesor prof = newProfesor("o@example.com");
        Trabajo trabajo = newTrabajoAbierto(prof);

        solicitudRepository.saveAndFlush(newSolicitud(trabajo, est, EstadoSolicitud.PENDIENTE));

        SolicitudVinculacion duplicada = newSolicitud(trabajo, est, EstadoSolicitud.PENDIENTE);
        assertThatThrownBy(() -> solicitudRepository.saveAndFlush(duplicada))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void permite_solicitud_rechazada_y_nueva_pendiente() {
        Estudiante est = newEstudiante("t@example.com");
        Profesor prof = newProfesor("u@example.com");
        Trabajo trabajo = newTrabajoAbierto(prof);

        solicitudRepository.saveAndFlush(newSolicitud(trabajo, est, EstadoSolicitud.RECHAZADA));
        solicitudRepository.saveAndFlush(newSolicitud(trabajo, est, EstadoSolicitud.PENDIENTE));
    }

    private Estudiante newEstudiante(String email) {
        Estudiante e = new Estudiante();
        e.setEmail(email);
        e.setPassword("x");
        e.setNombre("E");
        return estudianteRepository.saveAndFlush(e);
    }

    private Profesor newProfesor(String email) {
        Profesor p = new Profesor();
        p.setEmail(email);
        p.setPassword("x");
        p.setNombre("P");
        return profesorRepository.saveAndFlush(p);
    }

    private Trabajo newTrabajoAbierto(Profesor prof) {
        Trabajo t = new Trabajo();
        t.setTitulo("T");
        t.setTipo(TipoTrabajo.TCC);
        t.setEstado(EstadoTrabajo.ABIERTO);
        t.setOrientador(prof);
        return trabajoRepository.saveAndFlush(t);
    }

    private SolicitudVinculacion newSolicitud(Trabajo trabajo, Estudiante est, EstadoSolicitud estado) {
        SolicitudVinculacion s = new SolicitudVinculacion();
        s.setTrabajo(trabajo);
        s.setEstudiante(est);
        s.setEstado(estado);
        return s;
    }
}
