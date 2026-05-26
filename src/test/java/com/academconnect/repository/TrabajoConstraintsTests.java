package com.academconnect.repository;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.academconnect.AbstractJpaTest;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.Trabajo;

public class TrabajoConstraintsTests extends AbstractJpaTest {

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private TrabajoRepository trabajoRepository;

    @Test
    void saveShouldThrowDataIntegrityViolationExceptionWhenTwoActiveTrabajosOfSameTipoForSameEstudiante() {
        Estudiante est = newEstudiante("a@example.com");
        Profesor prof = newProfesor("p@example.com");

        Trabajo t1 = newTrabajo(prof, est, TipoTrabajo.TCC, EstadoTrabajo.EN_DESARROLLO);
        trabajoRepository.saveAndFlush(t1);

        Trabajo t2 = newTrabajo(prof, est, TipoTrabajo.TCC, EstadoTrabajo.EN_DESARROLLO);

        Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> trabajoRepository.saveAndFlush(t2));
    }

    @Test
    void saveShouldAllowActiveTrabajoWhenPreviousIsFinalizadoForSameTipo() {
        Estudiante est = newEstudiante("b@example.com");
        Profesor prof = newProfesor("q@example.com");

        trabajoRepository.saveAndFlush(newTrabajo(prof, est, TipoTrabajo.TCC, EstadoTrabajo.RECHAZADO));
        trabajoRepository.saveAndFlush(newTrabajo(prof, est, TipoTrabajo.TCC, EstadoTrabajo.EN_DESARROLLO));

        Assertions.assertEquals(2, trabajoRepository.findAll().size());
    }

    @Test
    void saveShouldAllowSimultaneousActiveTrabajosWhenTiposAreDifferent() {
        Estudiante est = newEstudiante("c@example.com");
        Profesor prof = newProfesor("r@example.com");

        trabajoRepository.saveAndFlush(newTrabajo(prof, est, TipoTrabajo.TCC, EstadoTrabajo.EN_DESARROLLO));
        trabajoRepository.saveAndFlush(newTrabajo(prof, est, TipoTrabajo.PAPER, EstadoTrabajo.EN_DESARROLLO));

        Assertions.assertEquals(2, trabajoRepository.findAll().size());
    }

    private Estudiante newEstudiante(String email) {
        Estudiante e = new Estudiante();
        e.setEmail(email);
        e.setPassword("x");
        e.setNombre("E " + email);
        return estudianteRepository.saveAndFlush(e);
    }

    private Profesor newProfesor(String email) {
        Profesor p = new Profesor();
        p.setEmail(email);
        p.setPassword("x");
        p.setNombre("P " + email);
        return profesorRepository.saveAndFlush(p);
    }

    private Trabajo newTrabajo(Profesor prof, Estudiante est, TipoTrabajo tipo, EstadoTrabajo estado) {
        Trabajo t = new Trabajo();
        t.setTitulo("T " + tipo + " " + estado);
        t.setTipo(tipo);
        t.setEstado(estado);
        t.setOrientador(prof);
        t.setEstudiante(est);
        t.setKeywords(List.of("kw1", "kw2", "kw3"));
        return t;
    }
}
