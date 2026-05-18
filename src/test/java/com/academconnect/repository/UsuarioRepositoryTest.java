package com.academconnect.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.academconnect.AbstractJpaTest;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.Externo;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.Rol;

class UsuarioRepositoryTest extends AbstractJpaTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private ExternoRepository externoRepository;

    @Test
    void persiste_subclases_via_herencia_JOINED() {
        Estudiante e = new Estudiante();
        e.setEmail("est@example.com");
        e.setPassword("x");
        e.setNombre("Estudiante Uno");
        estudianteRepository.save(e);

        Profesor p = new Profesor();
        p.setEmail("prof@example.com");
        p.setPassword("x");
        p.setNombre("Profesora Dos");
        p.setTitulacion("Doutora");
        profesorRepository.save(p);

        Externo x = new Externo();
        x.setEmail("ext@example.com");
        x.setPassword("x");
        x.setNombre("Externa Tres");
        x.setInstitucion("UFPel");
        x.setTitulo("PhD");
        externoRepository.save(x);

        assertThat(usuarioRepository.findAll()).hasSize(3);
        assertThat(usuarioRepository.findByEmail("prof@example.com"))
                .get()
                .isInstanceOf(Profesor.class)
                .extracting(u -> u.getRol())
                .isEqualTo(Rol.PROFESOR);
    }

    @Test
    void email_unico_se_respeta() {
        Estudiante e1 = new Estudiante();
        e1.setEmail("dup@example.com");
        e1.setPassword("x");
        e1.setNombre("A");
        estudianteRepository.saveAndFlush(e1);

        Profesor e2 = new Profesor();
        e2.setEmail("dup@example.com");
        e2.setPassword("x");
        e2.setNombre("B");

        assertThatThrownBy(() -> profesorRepository.saveAndFlush(e2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
