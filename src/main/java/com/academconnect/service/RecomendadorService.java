package com.academconnect.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.EstadoAsignacion;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.RecomendacionEvaluador;
import com.academconnect.domain.Usuario;
import com.academconnect.dto.SugerenciaEvaluadorResponse;
import com.academconnect.dto.SugerenciaOrientadorResponse;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.ConflictoInteresRepository;
import com.academconnect.repository.ExternoRepository;
import com.academconnect.repository.ProfesorRepository;
import com.academconnect.repository.RecomendacionEvaluadorRepository;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.repository.UsuarioAreaTematicaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecomendadorService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final List<EstadoTrabajo> ESTADOS_FINALIZADOS = List.of(
            EstadoTrabajo.APROBADO,
            EstadoTrabajo.RECHAZADO,
            EstadoTrabajo.CANCELADO);

    @Value("${academconnect.algoritmo.w1:0.6}")
    private double w1;

    @Value("${academconnect.algoritmo.w2:0.3}")
    private double w2;

    @Value("${academconnect.algoritmo.w3:0.1}")
    private double w3;

    @Value("${academconnect.algoritmo.orientador.w1:0.7}")
    private double wo1;

    @Value("${academconnect.algoritmo.orientador.w2:0.3}")
    private double wo2;

    private final TrabajoRepository trabajoRepository;
    private final ProfesorRepository profesorRepository;
    private final ExternoRepository externoRepository;
    private final UsuarioAreaTematicaRepository uatRepository;
    private final AsignacionRepository asignacionRepository;
    private final ConflictoInteresRepository conflictoRepository;
    private final RecomendacionEvaluadorRepository recomendacionRepository;

    @Transactional
    public List<SugerenciaEvaluadorResponse> sugerirRevisores(Long trabajoId, int k) {
        var trabajo = trabajoRepository.findById(trabajoId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", trabajoId));

        Set<Long> areasTrabajoIds = trabajo.getAreas().stream()
                .map(a -> a.getId())
                .collect(Collectors.toSet());

        List<Usuario> candidatos = new ArrayList<>();
        candidatos.addAll(profesorRepository.findByActivo(true));
        candidatos.addAll(externoRepository.findByActivo(true));

        candidatos = candidatos.stream()
                .filter(c -> !c.getId().equals(trabajo.getOrientador().getId()))
                .filter(c -> !conflictoRepository.existsByTrabajoIdAndEvaluadorId(trabajoId, c.getId()))
                .toList();

        Map<Long, Long> cargas = candidatos.stream()
                .collect(Collectors.toMap(
                        Usuario::getId,
                        c -> asignacionRepository.countByEvaluadorIdAndEstado(c.getId(), EstadoAsignacion.ACTIVA)));

        long maxCarga = cargas.values().stream().max(Comparator.naturalOrder()).orElse(0L);

        List<CandidatoScore> scored = candidatos.stream()
                .map(c -> puntuar(c, areasTrabajoIds, cargas.get(c.getId()), maxCarga))
                .sorted(Comparator.comparingDouble(CandidatoScore::score).reversed())
                .limit(k)
                .toList();

        persistirRecomendaciones(trabajoId, trabajo, scored);

        return scored.stream().map(this::toResponse).toList();
    }

    private CandidatoScore puntuar(
            Usuario candidato,
            Set<Long> areasTrabajoIds,
            long cargaAbsoluta,
            long maxCarga) {

        Set<Long> areasEval = uatRepository.findByIdUsuarioId(candidato.getId()).stream()
                .map(uat -> uat.getId().getAreaId())
                .collect(Collectors.toSet());

        double afinidad = jaccard(areasTrabajoIds, areasEval);
        double cargaNorm = maxCarga == 0 ? 0.0 : (double) cargaAbsoluta / maxCarga;
        double disponibilidad = 1.0;
        double score = w1 * afinidad + w2 * (1.0 - cargaNorm) + w3 * disponibilidad;

        return new CandidatoScore(candidato, score, afinidad, cargaNorm, disponibilidad);
    }

    private void persistirRecomendaciones(
            Long trabajoId,
            com.academconnect.domain.Trabajo trabajo,
            List<CandidatoScore> scored) {

        recomendacionRepository.deleteAll(
                recomendacionRepository.findByTrabajoIdOrderByScoreDesc(trabajoId));

        Instant ahora = Instant.now();
        List<RecomendacionEvaluador> entidades = scored.stream().map(cs -> {
            var rec = new RecomendacionEvaluador();
            rec.setTrabajo(trabajo);
            rec.setEvaluadorCandidato(cs.candidato());
            rec.setScore(bd4(cs.score()));
            rec.setFactores(factoresJson(cs));
            rec.setGeneradaEn(ahora);
            return rec;
        }).toList();

        recomendacionRepository.saveAll(entidades);
    }

    @Transactional(readOnly = true)
    public List<SugerenciaOrientadorResponse> sugerirOrientadores(Long trabajoId) {
        var trabajo = trabajoRepository.findById(trabajoId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", trabajoId));

        Set<Long> areasTrabajoIds = trabajo.getAreas().stream()
                .map(AreaTematica::getId)
                .collect(Collectors.toSet());

        Long orientadorActualId = trabajo.getOrientador() == null
                ? null : trabajo.getOrientador().getId();

        List<Profesor> candidatos = profesorRepository.findByActivo(true).stream()
                .filter(p -> orientadorActualId == null || !p.getId().equals(orientadorActualId))
                .toList();

        Map<Long, Long> cargas = candidatos.stream()
                .collect(Collectors.toMap(
                        Profesor::getId,
                        p -> trabajoRepository.countByOrientadorIdAndEstadoNotIn(
                                p.getId(), ESTADOS_FINALIZADOS)));

        long maxCarga = cargas.values().stream().max(Comparator.naturalOrder()).orElse(0L);

        return candidatos.stream()
                .map(p -> puntuarOrientador(p, areasTrabajoIds, cargas.get(p.getId()), maxCarga))
                .sorted(Comparator.comparing(SugerenciaOrientadorResponse::score).reversed()
                        .thenComparing(SugerenciaOrientadorResponse::nombre))
                .toList();
    }

    private SugerenciaOrientadorResponse puntuarOrientador(
            Profesor p, Set<Long> areasTrabajoIds, long carga, long maxCarga) {

        var uats = uatRepository.findByIdUsuarioId(p.getId());
        Set<Long> areasProfe = uats.stream()
                .map(u -> u.getId().getAreaId())
                .collect(Collectors.toSet());
        List<String> areasNombres = uats.stream()
                .map(u -> u.getArea() == null ? null : u.getArea().getNombre())
                .filter(java.util.Objects::nonNull)
                .sorted()
                .toList();

        double afinidad = jaccard(areasTrabajoIds, areasProfe);
        double cargaNorm = maxCarga == 0 ? 0.0 : (double) carga / maxCarga;
        double score = wo1 * afinidad + wo2 * (1.0 - cargaNorm);

        return new SugerenciaOrientadorResponse(
                p.getId(), p.getNombre(), p.getEmail(),
                areasNombres, carga, bd4(afinidad), bd4(score));
    }

    private double jaccard(Set<Long> a, Set<Long> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        Set<Long> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<Long> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    private String factoresJson(CandidatoScore cs) {
        try {
            return OBJECT_MAPPER.writeValueAsString(Map.of(
                    "afinidad", cs.afinidad(),
                    "carga_norm", cs.cargaNorm(),
                    "disponibilidad", cs.disponibilidad()));
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private SugerenciaEvaluadorResponse toResponse(CandidatoScore cs) {
        return new SugerenciaEvaluadorResponse(
                cs.candidato().getId(),
                cs.candidato().getNombre(),
                cs.candidato().getEmail(),
                cs.candidato().getRol(),
                bd4(cs.score()),
                bd4(cs.afinidad()),
                bd4(cs.cargaNorm()),
                bd4(cs.disponibilidad()));
    }

    private static BigDecimal bd4(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP);
    }

    private record CandidatoScore(
            Usuario candidato,
            double score,
            double afinidad,
            double cargaNorm,
            double disponibilidad) {
    }
}
