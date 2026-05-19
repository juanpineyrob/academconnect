package com.academconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.EstadoAsignacion;
import com.academconnect.domain.Externo;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.Trabajo;
import com.academconnect.domain.UsuarioAreaTematica;
import com.academconnect.domain.UsuarioAreaTematicaId;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.ConflictoInteresRepository;
import com.academconnect.repository.ExternoRepository;
import com.academconnect.repository.ProfesorRepository;
import com.academconnect.repository.RecomendacionEvaluadorRepository;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.repository.UsuarioAreaTematicaRepository;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class RecomendadorServiceTest {

    @Mock TrabajoRepository trabajoRepository;
    @Mock ProfesorRepository profesorRepository;
    @Mock ExternoRepository externoRepository;
    @Mock UsuarioAreaTematicaRepository uatRepository;
    @Mock AsignacionRepository asignacionRepository;
    @Mock ConflictoInteresRepository conflictoRepository;
    @Mock RecomendacionEvaluadorRepository recomendacionRepository;

    RecomendadorService service;

    @BeforeEach
    void setUp() {
        service = new RecomendadorService(trabajoRepository, profesorRepository, externoRepository,
                uatRepository, asignacionRepository, conflictoRepository, recomendacionRepository);
        ReflectionTestUtils.setField(service, "w1", 0.6);
        ReflectionTestUtils.setField(service, "w2", 0.3);
        ReflectionTestUtils.setField(service, "w3", 0.1);
    }

    @Test
    void candidato_con_mismas_areas_que_trabajo_recibe_maxima_afinidad() {
        var area1 = area(10L);
        var area2 = area(20L);

        var trabajo = trabajo(Set.of(area1, area2));
        var profesor = profesor(1L);

        when(trabajoRepository.findById(99L)).thenReturn(Optional.of(trabajo));
        when(profesorRepository.findByActivo(true)).thenReturn(List.of(profesor));
        when(externoRepository.findByActivo(true)).thenReturn(List.of());
        when(conflictoRepository.existsByTrabajoIdAndEvaluadorId(any(), eq(1L))).thenReturn(false);
        when(asignacionRepository.countByEvaluadorIdAndEstado(1L, EstadoAsignacion.ACTIVA)).thenReturn(0L);
        when(uatRepository.findByIdUsuarioId(1L)).thenReturn(List.of(uat(1L, 10L), uat(1L, 20L)));
        when(recomendacionRepository.findByTrabajoIdOrderByScoreDesc(99L)).thenReturn(List.of());
        when(recomendacionRepository.saveAll(any())).thenReturn(List.of());

        var sugerencias = service.sugerirRevisores(99L, 3);

        assertThat(sugerencias).hasSize(1);
        // afinidad=1.0, carga_norm=0.0, disponibilidad=1.0 → score=0.6+0.3+0.1=1.0
        assertThat(sugerencias.get(0).score().doubleValue()).isCloseTo(1.0, within(0.001));
        assertThat(sugerencias.get(0).afinidad().doubleValue()).isCloseTo(1.0, within(0.001));
    }

    @Test
    void candidato_sin_areas_comunes_con_trabajo_recibe_afinidad_cero() {
        var area1 = area(10L);
        var area2 = area(20L);
        var areaSoloProfe = area(30L);

        var trabajo = trabajo(Set.of(area1, area2));
        var profesor = profesor(1L);

        when(trabajoRepository.findById(99L)).thenReturn(Optional.of(trabajo));
        when(profesorRepository.findByActivo(true)).thenReturn(List.of(profesor));
        when(externoRepository.findByActivo(true)).thenReturn(List.of());
        when(conflictoRepository.existsByTrabajoIdAndEvaluadorId(any(), eq(1L))).thenReturn(false);
        when(asignacionRepository.countByEvaluadorIdAndEstado(1L, EstadoAsignacion.ACTIVA)).thenReturn(0L);
        when(uatRepository.findByIdUsuarioId(1L)).thenReturn(List.of(uat(1L, 30L)));
        when(recomendacionRepository.findByTrabajoIdOrderByScoreDesc(99L)).thenReturn(List.of());
        when(recomendacionRepository.saveAll(any())).thenReturn(List.of());

        var sugerencias = service.sugerirRevisores(99L, 3);

        assertThat(sugerencias.get(0).afinidad().doubleValue()).isCloseTo(0.0, within(0.001));
    }

    @Test
    void candidato_con_coi_es_excluido() {
        var trabajo = trabajo(Set.of());
        var profesor = profesor(1L);

        when(trabajoRepository.findById(99L)).thenReturn(Optional.of(trabajo));
        when(profesorRepository.findByActivo(true)).thenReturn(List.of(profesor));
        when(externoRepository.findByActivo(true)).thenReturn(List.of());
        when(conflictoRepository.existsByTrabajoIdAndEvaluadorId(99L, 1L)).thenReturn(true);
        when(recomendacionRepository.findByTrabajoIdOrderByScoreDesc(99L)).thenReturn(List.of());
        when(recomendacionRepository.saveAll(any())).thenReturn(List.of());

        var sugerencias = service.sugerirRevisores(99L, 3);

        assertThat(sugerencias).isEmpty();
    }

    @Test
    void k_limita_el_numero_de_sugerencias() {
        var trabajo = trabajo(Set.of());

        var p1 = profesor(1L);
        var p2 = profesor(2L);
        var p3 = profesor(3L);
        var p4 = profesor(4L);

        when(trabajoRepository.findById(99L)).thenReturn(Optional.of(trabajo));
        when(profesorRepository.findByActivo(true)).thenReturn(List.of(p1, p2, p3, p4));
        when(externoRepository.findByActivo(true)).thenReturn(List.of());
        when(conflictoRepository.existsByTrabajoIdAndEvaluadorId(any(), any())).thenReturn(false);
        when(asignacionRepository.countByEvaluadorIdAndEstado(any(), eq(EstadoAsignacion.ACTIVA))).thenReturn(0L);
        when(uatRepository.findByIdUsuarioId(any())).thenReturn(List.of());
        when(recomendacionRepository.findByTrabajoIdOrderByScoreDesc(99L)).thenReturn(List.of());
        when(recomendacionRepository.saveAll(any())).thenReturn(List.of());

        var sugerencias = service.sugerirRevisores(99L, 2);

        assertThat(sugerencias).hasSize(2);
    }

    // ---- helpers ----

    private static AreaTematica area(Long id) {
        var a = new AreaTematica();
        ReflectionTestUtils.setField(a, "id", id);
        return a;
    }

    private static Trabajo trabajo(Set<AreaTematica> areas) {
        var t = new Trabajo();
        ReflectionTestUtils.setField(t, "id", 99L);
        ReflectionTestUtils.setField(t, "areas", new HashSet<>(areas));
        var orientador = new Profesor();
        ReflectionTestUtils.setField(orientador, "id", 999L);
        t.setOrientador(orientador);
        return t;
    }

    private static Profesor profesor(Long id) {
        var p = new Profesor();
        ReflectionTestUtils.setField(p, "id", id);
        p.setNombre("Profesor " + id);
        p.setEmail("prof" + id + "@test.com");
        return p;
    }

    private static UsuarioAreaTematica uat(Long usuarioId, Long areaId) {
        var uat = new UsuarioAreaTematica();
        ReflectionTestUtils.setField(uat, "id", new UsuarioAreaTematicaId(usuarioId, areaId));
        return uat;
    }
}
