package com.academconnect.repository;

import java.time.Year;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.academconnect.AbstractJpaTest;
import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.ThesaurusOrigen;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.Trabajo;
import com.academconnect.repository.spec.TrabajoSpecs;

/** F13 — G12+G13: combina Specifications para validar la búsqueda multi-parámetro. */
public class TrabajoBuscarSpecTests extends AbstractJpaTest {

    @Autowired
    private TrabajoRepository trabajoRepository;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private AreaTematicaRepository areaRepository;

    @Test
    void findAllShouldReturnFilteredWhenCombinedAreaTipoAndEstado() {
        Profesor prof = profesor("p1@x.com");

        AreaTematica ia = area("IA");
        AreaTematica bd = area("BD");

        Trabajo tA = trabajo(prof, TipoTrabajo.TCC, EstadoTrabajo.APROBADO, "alfa", ia);
        trabajo(prof, TipoTrabajo.TCC, EstadoTrabajo.EN_DESARROLLO, "beta", ia);
        trabajo(prof, TipoTrabajo.PAPER, EstadoTrabajo.APROBADO, "gamma", bd);

        // Filtra: TCC + APROBADO + área IA → solo tA
        Specification<Trabajo> spec = ((Specification<Trabajo>) (root, cq, cb) -> cb.conjunction())
                .and(TrabajoSpecs.tipoIgual(TipoTrabajo.TCC))
                .and(TrabajoSpecs.estadoIgual(EstadoTrabajo.APROBADO))
                .and(TrabajoSpecs.tieneAlgunArea(List.of(ia.getId())));

        Page<Trabajo> page = trabajoRepository.findAll(spec, PageRequest.of(0, 10));
        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(tA.getId(), page.getContent().get(0).getId());
    }

    @Test
    void findAllShouldReturnOnlyTrabajosCreatedInGivenYearWhenFilteredByAnio() {
        Profesor prof = profesor("p2@x.com");
        Trabajo t = trabajo(prof, TipoTrabajo.TCC, EstadoTrabajo.APROBADO, "delta");

        int anioActual = Year.now().getValue();
        Specification<Trabajo> spec = ((Specification<Trabajo>) (root, cq, cb) -> cb.conjunction())
                .and(TrabajoSpecs.creadoEnAnio(List.of(anioActual)));

        Page<Trabajo> page = trabajoRepository.findAll(spec, PageRequest.of(0, 10));
        Assertions.assertTrue(page.getContent().stream().anyMatch(x -> x.getId().equals(t.getId())));

        Page<Trabajo> pageOtro = trabajoRepository.findAll(
                ((Specification<Trabajo>) (root, cq, cb) -> cb.conjunction())
                        .and(TrabajoSpecs.creadoEnAnio(List.of(anioActual - 100))),
                PageRequest.of(0, 10));
        Assertions.assertTrue(pageOtro.getContent().isEmpty());
    }

    @Test
    void buscarIdsPorTextoShouldReturnMatchingIdsWhenFtsMatches() {
        Profesor prof = profesor("p3@x.com");
        Trabajo t1 = trabajo(prof, TipoTrabajo.TCC, EstadoTrabajo.APROBADO,
                "Sistema integral de evaluación de trabajos científicos");
        trabajo(prof, TipoTrabajo.TCC, EstadoTrabajo.APROBADO,
                "Estudio de redes neuronales");

        List<Long> ids = trabajoRepository.buscarIdsPorTexto("integral");
        Assertions.assertTrue(ids.contains(t1.getId()));
    }

    private Profesor profesor(String email) {
        Profesor p = new Profesor();
        p.setEmail(email);
        p.setPassword("x");
        p.setNombre("P " + email);
        return profesorRepository.saveAndFlush(p);
    }

    private AreaTematica area(String nombre) {
        AreaTematica a = new AreaTematica();
        a.setNombre(nombre);
        a.setCodigoExterno("EXT-" + nombre + System.nanoTime());
        a.setThesaurusOrigen(ThesaurusOrigen.CNPQ);
        a.setActivo(true);
        return areaRepository.saveAndFlush(a);
    }

    private Trabajo trabajo(Profesor prof, TipoTrabajo tipo, EstadoTrabajo estado, String titulo,
                            AreaTematica... areas) {
        Trabajo t = new Trabajo();
        t.setTitulo(titulo);
        t.setDescripcion("desc " + titulo);
        t.setTipo(tipo);
        t.setEstado(estado);
        t.setOrientador(prof);
        t.setKeywords(List.of("kw1", "kw2", "kw3"));
        for (AreaTematica a : areas) {
            t.getAreas().add(a);
        }
        return trabajoRepository.saveAndFlush(t);
    }
}
