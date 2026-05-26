package com.academconnect.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.EstadoSesionEvaluacion;
import com.academconnect.domain.ModoEvaluacion;
import com.academconnect.domain.SesionEvaluacion;
import com.academconnect.domain.Trabajo;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.VisibilidadActividad;
import com.academconnect.dto.SesionEvaluacionRequest;
import com.academconnect.dto.SesionEvaluacionResponse;
import com.academconnect.event.ActividadEvent;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.SesionEvaluacionMapper;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.SesionEvaluacionRepository;
import com.academconnect.repository.TipoTrabajoConfigRepository;
import com.academconnect.repository.TrabajoRepository;

import lombok.RequiredArgsConstructor;

/** F14 G20 — sesiones de evaluación para modos SINCRONO/HIBRIDO. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SesionEvaluacionService {

    private static final Set<ModoEvaluacion> MODOS_VALIDOS = Set.of(
            ModoEvaluacion.SINCRONO, ModoEvaluacion.HIBRIDO);

    private final SesionEvaluacionRepository repository;
    private final TrabajoRepository trabajoRepository;
    private final TipoTrabajoConfigRepository tipoConfigRepository;
    private final AsignacionRepository asignacionRepository;
    private final ApplicationEventPublisher events;
    private final SesionEvaluacionMapper mapper;

    public SesionEvaluacionResponse buscarPorId(Long id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("SesionEvaluacion", id));
    }

    public List<SesionEvaluacionResponse> listarPorTrabajo(Long trabajoId) {
        if (!trabajoRepository.existsById(trabajoId)) {
            throw new ResourceNotFoundException("Trabajo", trabajoId);
        }
        return repository.findByTrabajoId(trabajoId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public SesionEvaluacionResponse crear(SesionEvaluacionRequest request) {
        if (request.fechaProgramada().isBefore(java.time.Instant.now())) {
            throw new BusinessException("fechaProgramada debe ser futura");
        }
        Trabajo trabajo = trabajoRepository.findById(request.trabajoId())
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", request.trabajoId()));

        var config = tipoConfigRepository.findById(trabajo.getTipo())
                .orElseThrow(() -> new BusinessException(
                        "No hay configuración para TipoTrabajo " + trabajo.getTipo()));
        if (!MODOS_VALIDOS.contains(config.getModoEvaluacion())) {
            throw new BusinessException("El tipo de trabajo %s usa modo %s — no admite sesiones de evaluación"
                    .formatted(trabajo.getTipo(), config.getModoEvaluacion()));
        }

        SesionEvaluacion sesion = new SesionEvaluacion();
        sesion.setTrabajo(trabajo);
        sesion.setFechaProgramada(request.fechaProgramada());
        sesion.setDuracionMinutos(request.duracionMinutos());
        sesion.setUbicacion(request.ubicacion());
        sesion.setModalidad(request.modalidad());
        sesion.setUrlMeet(request.urlMeet());
        sesion.setEstado(EstadoSesionEvaluacion.PROGRAMADA);

        var saved = repository.save(sesion);

        events.publishEvent(ActividadEvent.of(
                TipoActividad.SESION_PROGRAMADA,
                trabajo.getOrientador() != null ? trabajo.getOrientador().getId() : null,
                "SESION_EVALUACION", saved.getId(),
                Map.of("trabajoId", trabajo.getId(),
                       "trabajoTitulo", trabajo.getTitulo(),
                       "fecha", request.fechaProgramada().toString()),
                VisibilidadActividad.PARTICIPANTES,
                trabajo.getEstudiante() != null
                        ? List.of(trabajo.getEstudiante().getId())
                        : List.of()));

        return toResponse(saved);
    }

    /**
     * G20 — vincula una asignación existente a esta sesión. La asignación debe pertenecer
     * al mismo trabajo que la sesión; el evaluador queda dentro de la sesión sincrónica.
     */
    @Transactional
    public SesionEvaluacionResponse vincularAsignacion(Long sesionId, Long asignacionId) {
        var sesion = repository.findById(sesionId)
                .orElseThrow(() -> new ResourceNotFoundException("SesionEvaluacion", sesionId));
        var asignacion = asignacionRepository.findById(asignacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Asignacion", asignacionId));

        if (!asignacion.getTrabajo().getId().equals(sesion.getTrabajo().getId())) {
            throw new BusinessException(
                    "La asignación pertenece a otro trabajo — no se puede vincular a esta sesión");
        }
        if (sesion.getEstado() == EstadoSesionEvaluacion.FINALIZADA
                || sesion.getEstado() == EstadoSesionEvaluacion.CANCELADA) {
            throw new BusinessException("No se pueden vincular asignaciones a una sesión finalizada o cancelada");
        }

        asignacion.setSesion(sesion);
        asignacionRepository.save(asignacion);
        return toResponse(sesion);
    }

    @Transactional
    public SesionEvaluacionResponse cambiarEstado(Long id, EstadoSesionEvaluacion nuevo) {
        var sesion = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SesionEvaluacion", id));
        sesion.setEstado(nuevo);
        return toResponse(repository.save(sesion));
    }

    private SesionEvaluacionResponse toResponse(SesionEvaluacion s) {
        return mapper.toResponse(s);
    }
}
