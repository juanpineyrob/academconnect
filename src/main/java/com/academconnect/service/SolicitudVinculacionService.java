package com.academconnect.service;

import com.academconnect.domain.EstadoSolicitud;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.SolicitudVinculacion;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.VisibilidadActividad;
import com.academconnect.dto.RespuestaSolicitudRequest;
import com.academconnect.dto.SolicitudVinculacionRequest;
import com.academconnect.dto.SolicitudVinculacionResponse;
import com.academconnect.event.ActividadEvent;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.SolicitudVinculacionMapper;
import com.academconnect.repository.EstudianteRepository;
import com.academconnect.repository.SolicitudVinculacionRepository;
import com.academconnect.repository.TrabajoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SolicitudVinculacionService {

    private final SolicitudVinculacionRepository solicitudRepository;
    private final TrabajoRepository trabajoRepository;
    private final EstudianteRepository estudianteRepository;
    private final SolicitudVinculacionMapper mapper;
    private final ApplicationEventPublisher events;

    public SolicitudVinculacionResponse buscarPorId(Long id) {
        return solicitudRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("SolicitudVinculacion", id));
    }

    public List<SolicitudVinculacionResponse> listarPorTrabajo(Long trabajoId) {
        if (!trabajoRepository.existsById(trabajoId)) {
            throw new ResourceNotFoundException("Trabajo", trabajoId);
        }
        return solicitudRepository.findByTrabajoId(trabajoId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<SolicitudVinculacionResponse> listarPorEstudiante(Long estudianteId) {
        if (!estudianteRepository.existsById(estudianteId)) {
            throw new ResourceNotFoundException("Estudiante", estudianteId);
        }
        return solicitudRepository.findByEstudianteId(estudianteId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public SolicitudVinculacionResponse crear(SolicitudVinculacionRequest request) {
        var trabajo = trabajoRepository.findById(request.trabajoId())
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", request.trabajoId()));
        var estudiante = estudianteRepository.findById(request.estudianteId())
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", request.estudianteId()));

        if (trabajo.getEstado() != EstadoTrabajo.ABIERTO) {
            throw new BusinessException("El trabajo no está disponible para solicitudes de vinculación");
        }
        if (solicitudRepository.existsByTrabajoIdAndEstudianteIdAndEstado(
                trabajo.getId(), estudiante.getId(), EstadoSolicitud.PENDIENTE)) {
            throw new BusinessException("Ya existe una solicitud pendiente para este trabajo y estudiante");
        }

        var solicitud = new SolicitudVinculacion();
        solicitud.setTrabajo(trabajo);
        solicitud.setEstudiante(estudiante);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setMotivo(request.motivo());
        var saved = solicitudRepository.save(solicitud);

        events.publishEvent(ActividadEvent.of(
                TipoActividad.SOLICITUD_VINCULACION_ENVIADA,
                estudiante.getId(),
                "SOLICITUD_VINCULACION", saved.getId(),
                Map.of("trabajoId", trabajo.getId(), "trabajoTitulo", trabajo.getTitulo()),
                VisibilidadActividad.PARTICIPANTES,
                trabajo.getOrientador() != null
                        ? List.of(estudiante.getId(), trabajo.getOrientador().getId())
                        : List.of(estudiante.getId())));
        return mapper.toResponse(saved);
    }

    @Transactional
    public SolicitudVinculacionResponse aceptar(Long id, RespuestaSolicitudRequest request) {
        var solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SolicitudVinculacion", id));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BusinessException("Solo se pueden aceptar solicitudes en estado PENDIENTE");
        }
        var trabajo = solicitud.getTrabajo();
        if (trabajo.getEstado() != EstadoTrabajo.ABIERTO) {
            throw new BusinessException("El trabajo ya no está disponible");
        }
        if (trabajo.getEstudiante() != null) {
            throw new BusinessException("El trabajo ya tiene un estudiante asignado");
        }

        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setRespuesta(request != null ? request.respuesta() : null);
        solicitud.setResueltaEn(Instant.now());

        trabajo.setEstudiante(solicitud.getEstudiante());
        trabajo.setEstado(EstadoTrabajo.EN_DESARROLLO);
        trabajoRepository.save(trabajo);

        var otrasPendientes = solicitudRepository.findByTrabajoIdAndEstado(trabajo.getId(), EstadoSolicitud.PENDIENTE);
        for (var otra : otrasPendientes) {
            if (!otra.getId().equals(id)) {
                otra.setEstado(EstadoSolicitud.RECHAZADA);
                otra.setRespuesta("Posición ocupada");
                otra.setResueltaEn(Instant.now());
            }
        }
        solicitudRepository.saveAll(otrasPendientes);

        var saved = solicitudRepository.save(solicitud);
        events.publishEvent(ActividadEvent.of(
                TipoActividad.SOLICITUD_VINCULACION_APROBADA,
                trabajo.getOrientador() != null ? trabajo.getOrientador().getId() : null,
                "SOLICITUD_VINCULACION", saved.getId(),
                Map.of("trabajoId", trabajo.getId(), "trabajoTitulo", trabajo.getTitulo()),
                VisibilidadActividad.PARTICIPANTES,
                List.of(solicitud.getEstudiante().getId(),
                        trabajo.getOrientador() != null ? trabajo.getOrientador().getId() : 0L)));
        return mapper.toResponse(saved);
    }

    @Transactional
    public SolicitudVinculacionResponse rechazar(Long id, RespuestaSolicitudRequest request) {
        var solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SolicitudVinculacion", id));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BusinessException("Solo se pueden rechazar solicitudes en estado PENDIENTE");
        }

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setRespuesta(request != null ? request.respuesta() : null);
        solicitud.setResueltaEn(Instant.now());

        var trabajo = solicitud.getTrabajo();
        var saved = solicitudRepository.save(solicitud);
        events.publishEvent(ActividadEvent.of(
                TipoActividad.SOLICITUD_VINCULACION_RECHAZADA,
                trabajo.getOrientador() != null ? trabajo.getOrientador().getId() : null,
                "SOLICITUD_VINCULACION", saved.getId(),
                Map.of("trabajoId", trabajo.getId(), "trabajoTitulo", trabajo.getTitulo()),
                VisibilidadActividad.PARTICIPANTES,
                List.of(solicitud.getEstudiante().getId(),
                        trabajo.getOrientador() != null ? trabajo.getOrientador().getId() : 0L)));
        return mapper.toResponse(saved);
    }
}
