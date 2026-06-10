package com.academconnect.repository;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.academconnect.AbstractJpaTest;
import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.ThesaurusOrigen;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.Trabajo;
import com.academconnect.dto.UsuarioAreaTematicaResponse;

public class TrabajoRepositoryTests extends AbstractJpaTest {

    @Autowired
    private TrabajoRepository trabajoRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private AreaTematicaRepository areaRepository;

    // -----------------------------------------------------------------------
    // areasDerivadas — casos principales
    // -----------------------------------------------------------------------

    @Test
    void areasDerivadas_devuelve_areas_de_trabajos_aprobados_ordenadas_por_count() {
        // arrange
        Estudiante alumno = newEstudiante("alumno-areas@test.com");
        Profesor prof = newProfesor("prof-areas@test.com");

        AreaTematica areaA = newArea("A");
        AreaTematica areaB = newArea("B");
        AreaTematica areaC = newArea("C");

        // trabajo APROBADO con áreas A y B
        newTrabajo(prof, alumno, EstadoTrabajo.APROBADO, "Trabajo con A y B", areaA, areaB);
        // trabajo APROBADO con sólo área A → A queda con count 2
        newTrabajo(prof, alumno, EstadoTrabajo.APROBADO, "Trabajo solo A", areaA);
        // trabajo EN_DESARROLLO con área C → debe quedar EXCLUIDO
        newTrabajo(prof, alumno, EstadoTrabajo.EN_DESARROLLO, "Trabajo en desarrollo C", areaC);

        // act
        List<UsuarioAreaTematicaResponse> result = trabajoRepository.areasDerivadas(alumno.getId());

        // assert
        Assertions.assertEquals(2, result.size(), "Solo áreas de trabajos APROBADOS");
        Assertions.assertEquals("A", result.get(0).areaNombre(), "A debe ser primera (count 2)");
        Assertions.assertEquals("B", result.get(1).areaNombre(), "B debe ser segunda (count 1)");
        Assertions.assertNull(result.get(0).nivelExperticia(), "nivelExperticia siempre null");
        Assertions.assertNull(result.get(1).nivelExperticia(), "nivelExperticia siempre null");
    }

    @Test
    void areasDerivadas_alumno_sin_trabajos_aprobados_devuelve_vacio() {
        // arrange
        Estudiante alumno = newEstudiante("alumno-sinAprob@test.com");
        Profesor prof = newProfesor("prof-sinAprob@test.com");

        AreaTematica areaX = newArea("X");
        newTrabajo(prof, alumno, EstadoTrabajo.EN_DESARROLLO, "Solo en desarrollo", areaX);

        // act
        List<UsuarioAreaTematicaResponse> result = trabajoRepository.areasDerivadas(alumno.getId());

        // assert
        Assertions.assertTrue(result.isEmpty(), "Sin trabajos APROBADOS el resultado debe estar vacío");
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

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

    private AreaTematica newArea(String nombre) {
        AreaTematica a = new AreaTematica();
        a.setNombre(nombre);
        a.setCodigoExterno("EXT-" + nombre + "-" + System.nanoTime());
        a.setThesaurusOrigen(ThesaurusOrigen.CNPQ);
        a.setActivo(true);
        return areaRepository.saveAndFlush(a);
    }

    private Trabajo newTrabajo(Profesor prof, Estudiante est, EstadoTrabajo estado,
                               String titulo, AreaTematica... areas) {
        Trabajo t = new Trabajo();
        t.setTitulo(titulo);
        t.setDescripcion("desc " + titulo);
        t.setTipo(TipoTrabajo.TCC);
        t.setEstado(estado);
        t.setOrientador(prof);
        t.setEstudiante(est);
        t.setKeywords(List.of("kw1", "kw2", "kw3"));
        for (AreaTematica a : areas) {
            t.getAreas().add(a);
        }
        return trabajoRepository.saveAndFlush(t);
    }
}
