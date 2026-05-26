package com.academconnect.repository;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.academconnect.AbstractJpaTest;
import com.academconnect.domain.Actividad;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.VisibilidadActividad;

/** F15 — valida la visibilidad del feed (actor / pública / participante). */
public class ActividadRepositoryTests extends AbstractJpaTest {

    @Autowired
    private ActividadRepository repo;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Test
    void feedDelUsuarioShouldReturnPublicasPropiasAndParticipantesWhenUsuarioIsActor() {
        Long u1 = nuevoUsuario("u1@x.com");
        Long u2 = nuevoUsuario("u2@x.com");
        Long u3 = nuevoUsuario("u3@x.com");
        Long u4 = nuevoUsuario("u4@x.com");
        Long me = nuevoUsuario("me@x.com");

        repo.saveAndFlush(actividad(u1, VisibilidadActividad.PRIVADA, List.of()));         // ajena privada
        repo.saveAndFlush(actividad(u2, VisibilidadActividad.PARTICIPANTES, List.of(me))); // soy participante
        repo.saveAndFlush(actividad(u3, VisibilidadActividad.PUBLICA, List.of()));         // pública
        repo.saveAndFlush(actividad(me, VisibilidadActividad.PRIVADA, List.of()));         // propia privada
        repo.saveAndFlush(actividad(u4, VisibilidadActividad.PARTICIPANTES, List.of(u1))); // ajena (no soy participante)

        List<Actividad> feed = repo.feedDelUsuario(me, PageRequest.of(0, 50));

        // Debe ver: u2 (participante), u3 (pública), me (propia). No debe ver: u1, u4.
        Assertions.assertEquals(3, feed.size());
        List<Long> actorIds = feed.stream().map(Actividad::getActorId).sorted().toList();
        Assertions.assertEquals(List.of(u2, u3, me).stream().sorted().toList(), actorIds);
    }

    private Long nuevoUsuario(String email) {
        Estudiante e = new Estudiante();
        e.setEmail(email);
        e.setPassword("x");
        e.setNombre("U " + email);
        return estudianteRepository.saveAndFlush(e).getId();
    }

    private Actividad actividad(Long actorId, VisibilidadActividad vis, List<Long> participantes) {
        Actividad a = new Actividad();
        a.setTipo(TipoActividad.TRABAJO_CREADO);
        a.setActorId(actorId);
        a.setRecursoTipo("TRABAJO");
        a.setRecursoId(1L);
        a.setPayload("{}");
        a.setVisibilidad(vis);
        a.setParticipantesIds(participantes);
        return a;
    }
}
