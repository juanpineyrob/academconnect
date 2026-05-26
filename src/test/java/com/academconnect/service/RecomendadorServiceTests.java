package com.academconnect.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.EstadoAsignacion;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.Trabajo;
import com.academconnect.dto.SugerenciaEvaluadorResponse;
import com.academconnect.factories.AreaTematicaFactory;
import com.academconnect.factories.TrabajoFactory;
import com.academconnect.factories.UsuarioFactory;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.ConflictoInteresRepository;
import com.academconnect.repository.ExternoRepository;
import com.academconnect.repository.ProfesorRepository;
import com.academconnect.repository.RecomendacionEvaluadorRepository;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.repository.UsuarioAreaTematicaRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RecomendadorServiceTests {

    @InjectMocks
    private RecomendadorService service;

    @Mock
    private TrabajoRepository trabajoRepository;

    @Mock
    private ProfesorRepository profesorRepository;

    @Mock
    private ExternoRepository externoRepository;

    @Mock
    private UsuarioAreaTematicaRepository uatRepository;

    @Mock
    private AsignacionRepository asignacionRepository;

    @Mock
    private ConflictoInteresRepository conflictoRepository;

    @Mock
    private RecomendacionEvaluadorRepository recomendacionRepository;

    private Long trabajoId;
    private AreaTematica area1, area2, otraArea;
    private Trabajo trabajoConAreas, trabajoSinAreas;
    private Profesor profesor1, profesor2, profesor3, profesor4;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "w1", 0.6);
        ReflectionTestUtils.setField(service, "w2", 0.3);
        ReflectionTestUtils.setField(service, "w3", 0.1);

        trabajoId = 99L;
        area1 = AreaTematicaFactory.createArea(10L);
        area2 = AreaTematicaFactory.createArea(20L);
        otraArea = AreaTematicaFactory.createArea(30L);

        trabajoConAreas = buildTrabajoConAreas(Set.of(area1, area2));
        trabajoSinAreas = buildTrabajoConAreas(Set.of());

        profesor1 = UsuarioFactory.createProfesor(1L, "prof1@academconnect.com");
        profesor2 = UsuarioFactory.createProfesor(2L, "prof2@academconnect.com");
        profesor3 = UsuarioFactory.createProfesor(3L, "prof3@academconnect.com");
        profesor4 = UsuarioFactory.createProfesor(4L, "prof4@academconnect.com");

        Mockito.when(externoRepository.findByActivo(true)).thenReturn(List.of());
        Mockito.when(recomendacionRepository.findByTrabajoIdOrderByScoreDesc(trabajoId)).thenReturn(List.of());
        Mockito.when(recomendacionRepository.saveAll(Mockito.any())).thenReturn(List.of());
    }

    @Test
    void sugerirRevisoresShouldReturnMaxAfinidadWhenCandidatoHasSameAreasAsTrabajo() {
        Mockito.when(trabajoRepository.findById(trabajoId)).thenReturn(Optional.of(trabajoConAreas));
        Mockito.when(profesorRepository.findByActivo(true)).thenReturn(List.of(profesor1));
        Mockito.when(conflictoRepository.existsByTrabajoIdAndEvaluadorId(Mockito.any(), Mockito.eq(1L)))
                .thenReturn(false);
        Mockito.when(asignacionRepository.countByEvaluadorIdAndEstado(1L, EstadoAsignacion.ACTIVA))
                .thenReturn(0L);
        Mockito.when(uatRepository.findByIdUsuarioId(1L)).thenReturn(List.of(
                AreaTematicaFactory.createUat(1L, 10L),
                AreaTematicaFactory.createUat(1L, 20L)));

        List<SugerenciaEvaluadorResponse> sugerencias = service.sugerirRevisores(trabajoId, 3);

        Assertions.assertEquals(1, sugerencias.size());
        // afinidad=1.0, carga_norm=0.0, disponibilidad=1.0 → score=0.6+0.3+0.1=1.0
        Assertions.assertEquals(1.0, sugerencias.get(0).score().doubleValue(), 0.001);
        Assertions.assertEquals(1.0, sugerencias.get(0).afinidad().doubleValue(), 0.001);
    }

    @Test
    void sugerirRevisoresShouldReturnZeroAfinidadWhenCandidatoHasNoSharedAreas() {
        Mockito.when(trabajoRepository.findById(trabajoId)).thenReturn(Optional.of(trabajoConAreas));
        Mockito.when(profesorRepository.findByActivo(true)).thenReturn(List.of(profesor1));
        Mockito.when(conflictoRepository.existsByTrabajoIdAndEvaluadorId(Mockito.any(), Mockito.eq(1L)))
                .thenReturn(false);
        Mockito.when(asignacionRepository.countByEvaluadorIdAndEstado(1L, EstadoAsignacion.ACTIVA))
                .thenReturn(0L);
        Mockito.when(uatRepository.findByIdUsuarioId(1L))
                .thenReturn(List.of(AreaTematicaFactory.createUat(1L, otraArea.getId())));

        List<SugerenciaEvaluadorResponse> sugerencias = service.sugerirRevisores(trabajoId, 3);

        Assertions.assertEquals(0.0, sugerencias.get(0).afinidad().doubleValue(), 0.001);
    }

    @Test
    void sugerirRevisoresShouldExcludeCandidatoWithConflictoDeInteres() {
        Mockito.when(trabajoRepository.findById(trabajoId)).thenReturn(Optional.of(trabajoSinAreas));
        Mockito.when(profesorRepository.findByActivo(true)).thenReturn(List.of(profesor1));
        Mockito.when(conflictoRepository.existsByTrabajoIdAndEvaluadorId(trabajoId, 1L)).thenReturn(true);

        List<SugerenciaEvaluadorResponse> sugerencias = service.sugerirRevisores(trabajoId, 3);

        Assertions.assertTrue(sugerencias.isEmpty());
    }

    @Test
    void sugerirRevisoresShouldLimitResultsByK() {
        Mockito.when(trabajoRepository.findById(trabajoId)).thenReturn(Optional.of(trabajoSinAreas));
        Mockito.when(profesorRepository.findByActivo(true))
                .thenReturn(List.of(profesor1, profesor2, profesor3, profesor4));
        Mockito.when(conflictoRepository.existsByTrabajoIdAndEvaluadorId(Mockito.any(), Mockito.any()))
                .thenReturn(false);
        Mockito.when(asignacionRepository.countByEvaluadorIdAndEstado(Mockito.any(), Mockito.eq(EstadoAsignacion.ACTIVA)))
                .thenReturn(0L);
        Mockito.when(uatRepository.findByIdUsuarioId(Mockito.any())).thenReturn(List.of());

        List<SugerenciaEvaluadorResponse> sugerencias = service.sugerirRevisores(trabajoId, 2);

        Assertions.assertEquals(2, sugerencias.size());
    }

    private Trabajo buildTrabajoConAreas(Set<AreaTematica> areas) {
        Profesor orientador = UsuarioFactory.createProfesor(999L, "orientador@academconnect.com");
        Trabajo t = TrabajoFactory.createTrabajo(
                trabajoId, com.academconnect.domain.TipoTrabajo.TCC,
                com.academconnect.domain.EstadoTrabajo.EN_EVALUACION, orientador);
        ReflectionTestUtils.setField(t, "areas", new HashSet<>(areas));
        return t;
    }
}
