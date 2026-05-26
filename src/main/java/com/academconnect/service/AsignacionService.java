package com.academconnect.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.Asignacion;
import com.academconnect.domain.EstadoAsignacion;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.dto.AsignacionRequest;
import com.academconnect.dto.AsignacionResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.AsignacionMapper;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.ConflictoInteresRepository;
import com.academconnect.repository.TemplateEvaluacionRepository;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.repository.VersionamientoRepository;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.VisibilidadActividad;
import com.academconnect.event.ActividadEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AsignacionService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AsignacionRepository asignacionRepository;
    private final TrabajoRepository trabajoRepository;
    private final VersionamientoRepository versionamientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TemplateEvaluacionRepository templateRepository;
    private final ConflictoInteresRepository conflictoRepository;
    private final AsignacionMapper mapper;
    private final ApplicationEventPublisher events;

    public AsignacionResponse buscarPorId(Long id) {
        return asignacionRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Asignacion", id));
    }

    public List<AsignacionResponse> listarPorTrabajo(Long trabajoId) {
        if (!trabajoRepository.existsById(trabajoId)) {
            throw new ResourceNotFoundException("Trabajo", trabajoId);
        }
        return asignacionRepository.findByTrabajoId(trabajoId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public AsignacionResponse crear(AsignacionRequest request) {
        var trabajo = trabajoRepository.findById(request.trabajoId())
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", request.trabajoId()));
        var version = versionamientoRepository.findById(request.versionamientoId())
                .orElseThrow(() -> new ResourceNotFoundException("Versionamiento", request.versionamientoId()));
        var evaluador = usuarioRepository.findById(request.evaluadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", request.evaluadorId()));
        var template = templateRepository.findById(request.templateEvaluacionId())
                .orElseThrow(() -> new ResourceNotFoundException("TemplateEvaluacion", request.templateEvaluacionId()));

        if (!version.getTrabajo().getId().equals(trabajo.getId())) {
            throw new BusinessException("La versión no pertenece al trabajo indicado");
        }
        if (!template.isActivo()) {
            throw new BusinessException("El template de evaluación no está activo");
        }
        if (conflictoRepository.existsByTrabajoIdAndEvaluadorId(trabajo.getId(), evaluador.getId())) {
            throw new BusinessException("El evaluador tiene conflicto de interés con este trabajo");
        }

        var asignacion = new Asignacion();
        asignacion.setTrabajo(trabajo);
        asignacion.setVersionamiento(version);
        asignacion.setEvaluador(evaluador);
        asignacion.setTemplateSnapshot(construirSnapshot(template.getCriterios(), template.getUmbralAprobacion()));
        asignacion.setAsignadaEn(Instant.now());
        asignacion.setVencimientoEn(request.vencimientoEn());
        asignacion.setEstado(EstadoAsignacion.ACTIVA);

        if (trabajo.getEstado() == EstadoTrabajo.EN_DESARROLLO) {
            trabajo.setEstado(EstadoTrabajo.EN_EVALUACION);
            trabajoRepository.save(trabajo);
        }

        var saved = asignacionRepository.save(asignacion);

        var participantes = new ArrayList<Long>();
        if (trabajo.getOrientador() != null) participantes.add(trabajo.getOrientador().getId());
        if (trabajo.getEstudiante() != null) participantes.add(trabajo.getEstudiante().getId());
        participantes.add(evaluador.getId());
        events.publishEvent(ActividadEvent.of(
                TipoActividad.ASIGNACION_CREADA,
                trabajo.getOrientador() != null ? trabajo.getOrientador().getId() : null,
                "ASIGNACION", saved.getId(),
                Map.of("trabajoId", trabajo.getId(),
                       "trabajoTitulo", trabajo.getTitulo(),
                       "evaluadorId", evaluador.getId(),
                       "evaluadorNombre", evaluador.getNombre()),
                VisibilidadActividad.PARTICIPANTES,
                participantes));

        return mapper.toResponse(saved);
    }

    public List<AsignacionResponse> listarMisAsignaciones(String email) {
        return listarMisAsignaciones(email, EstadoAsignacion.ACTIVA);
    }

    /** G09 — filtra por estado dado (default ACTIVA si caller no especifica). */
    public List<AsignacionResponse> listarMisAsignaciones(String email, EstadoAsignacion estado) {
        var evaluador = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email));
        return asignacionRepository.findByEvaluadorIdAndEstado(evaluador.getId(), estado)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    /** G09 — sin filtro de estado (historial completo del evaluador). */
    public List<AsignacionResponse> listarMisAsignacionesTodas(String email) {
        var evaluador = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email));
        return asignacionRepository.findByEvaluadorId(evaluador.getId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    /**
     * Snapshot inmutable del template al momento de asignar:
     * {"criterios": [...], "umbralAprobacion": 6.00}.
     */
    private String construirSnapshot(String criteriosJson, java.math.BigDecimal umbralAprobacion) {
        try {
            ObjectNode snapshot = OBJECT_MAPPER.createObjectNode();
            snapshot.set("criterios", OBJECT_MAPPER.readTree(criteriosJson));
            snapshot.put("umbralAprobacion", umbralAprobacion);
            return OBJECT_MAPPER.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Error al construir snapshot del template: " + e.getMessage());
        }
    }

    @Transactional
    public AsignacionResponse cancelar(Long id) {
        var asignacion = asignacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignacion", id));
        if (asignacion.getEstado() != EstadoAsignacion.ACTIVA) {
            throw new BusinessException("Solo se pueden cancelar asignaciones activas");
        }
        asignacion.setEstado(EstadoAsignacion.CANCELADA);
        return mapper.toResponse(asignacionRepository.save(asignacion));
    }
}
