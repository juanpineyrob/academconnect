package com.academconnect.repository;

import com.academconnect.AbstractJpaTest;
import com.academconnect.domain.EstadoInvitacion;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.InvitacionOrientacion;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.Trabajo;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class InvitacionOrientacionRepositoryTests extends AbstractJpaTest {

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private TrabajoRepository trabajoRepository;

    @Autowired
    private InvitacionOrientacionRepository invitacionRepository;

    @Test
    void noDejaDosPendientesSobreElMismoTrabajo() {
        Estudiante e = newEstudiante("alumno@x.uy");
        Profesor p1 = newProfesor("p1@x.uy");
        Profesor p2 = newProfesor("p2@x.uy");
        Trabajo t = newTrabajoBorrador(e);

        invitacionRepository.saveAndFlush(invitacion(t, p1, EstadoInvitacion.PENDIENTE));

        Assertions.assertThrows(DataIntegrityViolationException.class, () ->
                invitacionRepository.saveAndFlush(invitacion(t, p2, EstadoInvitacion.PENDIENTE)));
    }

    @Test
    void permitePendienteNuevaSiLaAnteriorNoEstaPendiente() {
        Estudiante e = newEstudiante("alumno2@x.uy");
        Profesor p1 = newProfesor("pp1@x.uy");
        Profesor p2 = newProfesor("pp2@x.uy");
        Trabajo t = newTrabajoBorrador(e);

        invitacionRepository.saveAndFlush(invitacion(t, p1, EstadoInvitacion.RECHAZADA));

        Assertions.assertDoesNotThrow(() ->
                invitacionRepository.saveAndFlush(invitacion(t, p2, EstadoInvitacion.PENDIENTE)));
    }

    private Estudiante newEstudiante(String email) {
        Estudiante e = new Estudiante();
        e.setEmail(email);
        e.setPassword("x");
        e.setNombre("E " + email);
        e.setActivo(true);
        return estudianteRepository.saveAndFlush(e);
    }

    private Profesor newProfesor(String email) {
        Profesor p = new Profesor();
        p.setEmail(email);
        p.setPassword("x");
        p.setNombre("P " + email);
        p.setActivo(true);
        p.setTopeAsignaciones(10);
        return profesorRepository.saveAndFlush(p);
    }

    private Trabajo newTrabajoBorrador(Estudiante e) {
        Trabajo t = new Trabajo();
        t.setTitulo("Borrador X");
        t.setTipo(TipoTrabajo.TCC);
        t.setEstado(EstadoTrabajo.BORRADOR);
        t.setEstudiante(e);
        t.setKeywords(List.of("seg", "devsecops", "ci"));
        return trabajoRepository.saveAndFlush(t);
    }

    private InvitacionOrientacion invitacion(Trabajo t, Profesor p, EstadoInvitacion estado) {
        InvitacionOrientacion i = new InvitacionOrientacion();
        i.setTrabajo(t);
        i.setProfesor(p);
        i.setEstado(estado);
        return i;
    }
}
