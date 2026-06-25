package com.academconnect.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.Asignacion;
import com.academconnect.domain.InstanciaEvaluacion;
import com.academconnect.domain.CalificacionCriterio;
import com.academconnect.domain.EstadoAsignacion;
import com.academconnect.domain.EstadoEvaluacion;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Evaluacion;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.Trabajo;
import com.academconnect.domain.VisibilidadActividad;
import com.academconnect.dto.CalificacionCriterioRequest;
import com.academconnect.dto.CalificacionCriterioResponse;
import com.academconnect.dto.EvaluacionRequest;
import com.academconnect.dto.EvaluacionResponse;
import com.academconnect.event.ActividadEvent;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.EvaluacionMapper;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.EvaluacionRepository;
import com.academconnect.repository.TrabajoRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EvaluacionService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TIPO_TEXTO = "TEXTO";

    private final EvaluacionRepository evaluacionRepository;
    private final AsignacionRepository asignacionRepository;
    private final TrabajoRepository trabajoRepository;
    private final EvaluacionMapper mapper;
    private final ApplicationEventPublisher events;
    private final InstanciaEvaluacionService instanciaEvaluacionService;

    public EvaluacionResponse buscarPorAsignacion(Long asignacionId) {
        return evaluacionRepository.findByAsignacionId(asignacionId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluacion para asignacion", asignacionId));
    }

    public EvaluacionResponse buscarPorId(Long id) {
        return evaluacionRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluacion", id));
    }

    /**
     * Lista las evaluaciones cerradas del trabajo para el estudiante dueño.
     * Los comentarios con {@code comentarioPrivado=true} se ocultan al estudiante (G18).
     */
    public List<EvaluacionResponse> listarNotasTrabajo(Long trabajoId, String estudianteEmail) {
        var trabajo = trabajoRepository.findById(trabajoId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", trabajoId));
        var estudiante = trabajo.getEstudiante();
        if (estudiante == null || !estudiante.getEmail().equals(estudianteEmail)) {
            throw new BusinessException("El trabajo no pertenece al estudiante autenticado");
        }
        return evaluacionRepository
                .findByAsignacionTrabajoIdAndEstado(trabajoId, EstadoEvaluacion.COMPLETADA)
                .stream()
                .map(mapper::toResponse)
                .map(EvaluacionService::ocultarComentariosPrivados)
                .toList();
    }

    @Transactional
    public EvaluacionResponse completar(EvaluacionRequest request) {
        var asignacion = asignacionRepository.findById(request.asignacionId())
                .orElseThrow(() -> new ResourceNotFoundException("Asignacion", request.asignacionId()));

        if (asignacion.getEstado() != EstadoAsignacion.ACTIVA) {
            throw new BusinessException("Solo se pueden completar evaluaciones de asignaciones activas");
        }
        if (evaluacionRepository.findByAsignacionId(asignacion.getId()).isPresent()) {
            throw new BusinessException("Ya existe una evaluación para esta asignación");
        }

        Snapshot snapshot = parsearSnapshot(asignacion.getTemplateSnapshot());
        Map<String, Double> pesos = snapshot.criterios().stream()
                .filter(c -> !TIPO_TEXTO.equals(c.get("tipo")))
                .collect(Collectors.toMap(
                        c -> (String) c.get("codigo"),
                        c -> ((Number) c.get("peso")).doubleValue()));

        validarCalificaciones(request.calificaciones(), pesos);

        BigDecimal calificacionFinal = calcularCalificacionFinal(request.calificaciones(), pesos);

        var evaluacion = new Evaluacion();
        evaluacion.setAsignacion(asignacion);
        evaluacion.setEstado(EstadoEvaluacion.COMPLETADA);
        evaluacion.setCalificacionFinal(calificacionFinal);
        evaluacion.setComentarioGeneral(request.comentarioGeneral());
        evaluacion.setCompletadaEn(Instant.now());

        for (CalificacionCriterioRequest cal : request.calificaciones()) {
            var criterio = new CalificacionCriterio();
            criterio.setEvaluacion(evaluacion);
            criterio.setCriterioCodigo(cal.criterioCodigo());
            criterio.setPuntaje(cal.puntaje());
            criterio.setComentario(cal.comentario());
            criterio.setComentarioPrivado(cal.comentarioPrivado() == null || cal.comentarioPrivado());
            evaluacion.getCalificaciones().add(criterio);
        }

        asignacion.setEstado(EstadoAsignacion.COMPLETADA);
        asignacionRepository.save(asignacion);

        var saved = evaluacionRepository.save(evaluacion);

        events.publishEvent(ActividadEvent.of(
                TipoActividad.EVALUACION_COMPLETADA,
                asignacion.getEvaluador().getId(),
                "EVALUACION", saved.getId(),
                java.util.Map.of(
                        "trabajoId", asignacion.getTrabajo().getId(),
                        "trabajoTitulo", asignacion.getTrabajo().getTitulo(),
                        "score", saved.getCalificacionFinal()),
                VisibilidadActividad.PARTICIPANTES,
                participantesDe(asignacion.getTrabajo())));

        agregarVeredicto(asignacion, snapshot.umbralAprobacion());

        return mapper.toResponse(saved);
    }

    private static java.util.List<Long> participantesDe(Trabajo t) {
        java.util.List<Long> ids = new java.util.ArrayList<>();
        if (t.getOrientador() != null) ids.add(t.getOrientador().getId());
        if (t.getEstudiante() != null) ids.add(t.getEstudiante().getId());
        return ids;
    }

    /**
     * G17 — Si esta era la última asignación ACTIVA para la versión evaluada (o instancia),
     * calcula promedio y delega el veredicto:
     * <ul>
     *   <li>Si la asignación pertenece a una {@link InstanciaEvaluacion}, delega a
     *       {@link InstanciaEvaluacionService#alAprobar} / {@link InstanciaEvaluacionService#alReprobar}.</li>
     *   <li>Si no (ronda única / legacy), fija directamente el estado del {@link Trabajo}.</li>
     * </ul>
     */
    private void agregarVeredicto(Asignacion asignacion, BigDecimal umbral) {
        InstanciaEvaluacion instancia = asignacion.getInstanciaEvaluacion();
        if (instancia != null) {
            long activas = asignacionRepository.countByInstanciaEvaluacionIdAndEstado(
                    instancia.getId(), EstadoAsignacion.ACTIVA);
            if (activas > 0) return;

            BigDecimal promedio = evaluacionRepository.promedioPorInstancia(instancia.getId());
            if (promedio == null) return;

            BigDecimal puntaje = promedio.setScale(2, RoundingMode.HALF_UP);
            if (puntaje.compareTo(umbral) >= 0) {
                instanciaEvaluacionService.alAprobar(instancia, puntaje);
            } else {
                instanciaEvaluacionService.alReprobar(instancia, puntaje);
            }
            return;
        }

        // ---- rama legacy (ronda única) sin cambios ----
        Long trabajoId = asignacion.getTrabajo().getId();
        Long versionId = asignacion.getVersionamiento().getId();

        long pendientes = asignacionRepository.countByTrabajoIdAndVersionamientoIdAndEstado(
                trabajoId, versionId, EstadoAsignacion.ACTIVA);
        if (pendientes > 0) {
            return;
        }

        BigDecimal promedio = evaluacionRepository.promedioPorTrabajoYVersion(trabajoId, versionId);
        if (promedio == null) {
            return;
        }

        BigDecimal puntajeAgregado = promedio.setScale(2, RoundingMode.HALF_UP);
        EstadoTrabajo veredicto = puntajeAgregado.compareTo(umbral) >= 0
                ? EstadoTrabajo.APROBADO
                : EstadoTrabajo.RECHAZADO;

        Trabajo trabajo = asignacion.getTrabajo();
        trabajo.setPuntajeAgregado(puntajeAgregado);
        trabajo.setEvaluadoEn(Instant.now());
        trabajo.setEstado(veredicto);
        trabajoRepository.save(trabajo);

        events.publishEvent(ActividadEvent.of(
                veredicto == EstadoTrabajo.APROBADO
                        ? TipoActividad.TRABAJO_APROBADO
                        : TipoActividad.TRABAJO_RECHAZADO,
                null,
                "TRABAJO", trabajo.getId(),
                java.util.Map.of("titulo", trabajo.getTitulo(), "puntaje", puntajeAgregado),
                VisibilidadActividad.PUBLICA,
                participantesDe(trabajo)));
    }

    private record Snapshot(List<Map<String, Object>> criterios, BigDecimal umbralAprobacion) {}

    @SuppressWarnings("unchecked")
    private Snapshot parsearSnapshot(String json) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode criteriosNode;
            BigDecimal umbral;
            if (root.isObject() && root.has("criterios")) {
                criteriosNode = root.get("criterios");
                if (!root.has("umbralAprobacion") || root.get("umbralAprobacion").isNull()) {
                    throw new BusinessException("Snapshot del template no incluye umbralAprobacion");
                }
                umbral = root.get("umbralAprobacion").decimalValue();
            } else {
                throw new BusinessException("Formato del snapshot inválido: se esperaba objeto con 'criterios' y 'umbralAprobacion'");
            }
            List<Map<String, Object>> criterios = OBJECT_MAPPER.convertValue(
                    criteriosNode, new TypeReference<List<Map<String, Object>>>() {});
            return new Snapshot(criterios, umbral);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Error al parsear el template snapshot: " + e.getMessage());
        }
    }

    private void validarCalificaciones(
            List<CalificacionCriterioRequest> calificaciones,
            Map<String, Double> pesos) {
        for (var cal : calificaciones) {
            if (!pesos.containsKey(cal.criterioCodigo())) {
                throw new BusinessException("Criterio desconocido en el template: " + cal.criterioCodigo());
            }
        }
        for (String codigo : pesos.keySet()) {
            boolean presente = calificaciones.stream()
                    .anyMatch(c -> c.criterioCodigo().equals(codigo));
            if (!presente) {
                throw new BusinessException("Falta calificación para el criterio: " + codigo);
            }
        }
    }

    private BigDecimal calcularCalificacionFinal(
            List<CalificacionCriterioRequest> calificaciones,
            Map<String, Double> pesos) {
        return calificaciones.stream()
                .filter(c -> pesos.containsKey(c.criterioCodigo()))
                .map(cal -> cal.puntaje().multiply(
                        BigDecimal.valueOf(pesos.get(cal.criterioCodigo()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static EvaluacionResponse ocultarComentariosPrivados(EvaluacionResponse e) {
        var saneadas = e.calificaciones().stream()
                .map(c -> c.comentarioPrivado()
                        ? new CalificacionCriterioResponse(c.id(), c.criterioCodigo(), c.puntaje(), null, true)
                        : c)
                .toList();
        return new EvaluacionResponse(
                e.id(), e.asignacionId(), e.estado(), e.calificacionFinal(),
                e.comentarioGeneral(), saneadas, e.completadaEn(), e.createdAt());
    }
}
