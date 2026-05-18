package com.academconnect.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.academconnect.AbstractJpaTest;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.Trabajo;

class TrabajoConstraintsTest extends AbstractJpaTest {

    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private TrabajoRepository trabajoRepository;

    @Test
    void no_permite_dos_trabajos_activos_del_mismo_tipo_por_estudiante() {
        Estudiante est = newEstudiante("a@example.com");
        Profesor prof = newProfesor("p@example.com");

        Trabajo t1 = newTrabajo(prof, est, TipoTrabajo.TCC, EstadoTrabajo.EN_DESARROLLO);
        trabajoRepository.saveAndFlush(t1);

        Trabajo t2 = newTrabajo(prof, est, TipoTrabajo.TCC, EstadoTrabajo.EN_DESARROLLO);

        assertThatThrownBy(() -> trabajoRepository.saveAndFlush(t2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void permite_trabajo_activo_y_uno_finalizado_del_mismo_tipo() {
        Estudiante est = newEstudiante("b@example.com");
        Profesor prof = newProfesor("q@example.com");

        trabajoRepository.saveAndFlush(newTrabajo(prof, est, TipoTrabajo.TCC, EstadoTrabajo.RECHAZADO));
        trabajoRepository.saveAndFlush(newTrabajo(prof, est, TipoTrabajo.TCC, EstadoTrabajo.EN_DESARROLLO));

        assertThat(trabajoRepository.findAll()).hasSize(2);
    }

    @Test
    void permite_trabajos_activos_de_tipos_distintos_simultaneamente() {
        Estudiante est = newEstudiante("c@example.com");
        Profesor prof = newProfesor("r@example.com");

        trabajoRepository.saveAndFlush(newTrabajo(prof, est, TipoTrabajo.TCC, EstadoTrabajo.EN_DESARROLLO));
        trabajoRepository.saveAndFlush(newTrabajo(prof, est, TipoTrabajo.PAPER, EstadoTrabajo.EN_DESARROLLO));

        assertThat(trabajoRepository.findAll()).hasSize(2);
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
        return t;
    }
}
