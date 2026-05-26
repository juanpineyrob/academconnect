package com.academconnect.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.academconnect.AbstractJpaTest;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.Externo;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.Rol;
import com.academconnect.domain.Usuario;

public class UsuarioRepositoryTests extends AbstractJpaTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private ExternoRepository externoRepository;

    @Test
    void saveShouldPersistJoinedInheritanceWhenSubclassesAreUsed() {
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

        Assertions.assertEquals(3, usuarioRepository.findAll().size());
        Usuario loaded = usuarioRepository.findByEmail("prof@example.com").orElseThrow();
        Assertions.assertInstanceOf(Profesor.class, loaded);
        Assertions.assertEquals(Rol.PROFESOR, loaded.getRol());
    }

    @Test
    void saveAndFlushShouldThrowDataIntegrityViolationExceptionWhenEmailIsDuplicate() {
        Estudiante e1 = new Estudiante();
        e1.setEmail("dup@example.com");
        e1.setPassword("x");
        e1.setNombre("A");
        estudianteRepository.saveAndFlush(e1);

        Profesor e2 = new Profesor();
        e2.setEmail("dup@example.com");
        e2.setPassword("x");
        e2.setNombre("B");

        Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> profesorRepository.saveAndFlush(e2));
    }
}
