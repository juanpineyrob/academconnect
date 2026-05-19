package com.academconnect.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.CalificacionCriterio;
import com.academconnect.domain.EstadoAsignacion;
import com.academconnect.domain.EstadoEvaluacion;
import com.academconnect.domain.Evaluacion;
import com.academconnect.dto.CalificacionCriterioRequest;
import com.academconnect.dto.EvaluacionRequest;
import com.academconnect.dto.EvaluacionResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.EvaluacionMapper;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.EvaluacionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EvaluacionService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final EvaluacionRepository evaluacionRepository;
    private final AsignacionRepository asignacionRepository;
    private final EvaluacionMapper mapper;

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

        List<Map<String, Object>> criteriosSnapshot = parsearCriterios(asignacion.getTemplateSnapshot());
        Map<String, Double> pesos = criteriosSnapshot.stream()
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
            evaluacion.getCalificaciones().add(criterio);
        }

        asignacion.setEstado(EstadoAsignacion.COMPLETADA);
        asignacionRepository.save(asignacion);

        return mapper.toResponse(evaluacionRepository.save(evaluacion));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parsearCriterios(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
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
                .map(cal -> cal.puntaje().multiply(
                        BigDecimal.valueOf(pesos.get(cal.criterioCodigo()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
