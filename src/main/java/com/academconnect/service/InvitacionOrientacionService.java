package com.academconnect.service;

import com.academconnect.domain.EstadoInvitacion;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.InvitacionOrientacion;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.VisibilidadActividad;
import com.academconnect.dto.InvitacionOrientacionRequest;
import com.academconnect.dto.InvitacionOrientacionResponse;
import com.academconnect.dto.RespuestaInvitacionRequest;
import com.academconnect.event.ActividadEvent;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.InvitacionOrientacionMapper;
import com.academconnect.repository.InvitacionOrientacionRepository;
import com.academconnect.repository.ProfesorRepository;
import com.academconnect.repository.TrabajoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InvitacionOrientacionService {

    private final InvitacionOrientacionRepository repository;
    private final TrabajoRepository trabajoRepository;
    private final ProfesorRepository profesorRepository;
    private final InvitacionOrientacionMapper mapper;
    private final ApplicationEventPublisher events;

    @Transactional
    public InvitacionOrientacionResponse crear(InvitacionOrientacionRequest request, Long estudianteId) {
        var trabajo = trabajoRepository.findById(request.trabajoId())
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", request.trabajoId()));
        if (trabajo.getEstudiante() == null || !trabajo.getEstudiante().getId().equals(estudianteId)) {
            throw new BusinessException("No sos el dueño de este trabajo");
        }
        if (trabajo.getEstado() != EstadoTrabajo.BORRADOR) {
            throw new BusinessException("Solo se puede invitar orientador desde un BORRADOR");
        }
        if (trabajo.getOrientador() != null) {
            throw new BusinessException("El trabajo ya tiene orientador");
        }
        if (repository.existsByTrabajoIdAndEstado(trabajo.getId(), EstadoInvitacion.PENDIENTE)) {
            throw new BusinessException("Ya hay una invitación pendiente para este trabajo");
        }
        var profesor = profesorRepository.findById(request.profesorId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesor", request.profesorId()));
        if (!profesor.isActivo()) {
            throw new BusinessException("El profesor no está activo");
        }

        var invitacion = new InvitacionOrientacion();
        invitacion.setTrabajo(trabajo);
        invitacion.setProfesor(profesor);
        invitacion.setEstado(EstadoInvitacion.PENDIENTE);
        invitacion.setMotivo(request.motivo());
        var saved = repository.save(invitacion);

        events.publishEvent(ActividadEvent.of(
                TipoActividad.INVITACION_ORIENTACION_ENVIADA,
                estudianteId,
                "INVITACION_ORIENTACION", saved.getId(),
                Map.of("trabajoId", trabajo.getId(), "profesorId", profesor.getId(),
                        "trabajoTitulo", trabajo.getTitulo()),
                VisibilidadActividad.PARTICIPANTES,
                List.of(estudianteId, profesor.getId())));
        return mapper.toResponse(saved);
    }

    @Transactional
    public InvitacionOrientacionResponse aceptar(Long invitacionId, RespuestaInvitacionRequest request, Long profesorId) {
        var i = repository.findById(invitacionId)
                .orElseThrow(() -> new ResourceNotFoundException("InvitacionOrientacion", invitacionId));
        if (!i.getProfesor().getId().equals(profesorId)) {
            throw new BusinessException("Solo el profesor invitado puede aceptar");
        }
        if (i.getEstado() != EstadoInvitacion.PENDIENTE) {
            throw new BusinessException("La invitación ya fue resuelta");
        }

        i.setEstado(EstadoInvitacion.ACEPTADA);
        i.setRespuesta(request != null ? request.respuesta() : null);
        i.setResueltaEn(Instant.now());

        var trabajo = i.getTrabajo();
        trabajo.setOrientador(i.getProfesor());
        trabajo.setEstado(EstadoTrabajo.EN_DESARROLLO);

        var saved = repository.save(i);
        trabajoRepository.save(trabajo);

        events.publishEvent(ActividadEvent.of(
                TipoActividad.INVITACION_ORIENTACION_ACEPTADA,
                profesorId,
                "INVITACION_ORIENTACION", saved.getId(),
                Map.of("trabajoId", trabajo.getId(), "trabajoTitulo", trabajo.getTitulo()),
                VisibilidadActividad.PARTICIPANTES,
                List.of(profesorId, trabajo.getEstudiante().getId())));
        return mapper.toResponse(saved);
    }

    @Transactional
    public InvitacionOrientacionResponse rechazar(Long invitacionId, RespuestaInvitacionRequest request, Long profesorId) {
        var i = repository.findById(invitacionId)
                .orElseThrow(() -> new ResourceNotFoundException("InvitacionOrientacion", invitacionId));
        if (!i.getProfesor().getId().equals(profesorId)) {
            throw new BusinessException("Solo el profesor invitado puede rechazar");
        }
        if (i.getEstado() != EstadoInvitacion.PENDIENTE) {
            throw new BusinessException("La invitación ya fue resuelta");
        }
        i.setEstado(EstadoInvitacion.RECHAZADA);
        i.setRespuesta(request != null ? request.respuesta() : null);
        i.setResueltaEn(Instant.now());
        var saved = repository.save(i);
        events.publishEvent(ActividadEvent.of(
                TipoActividad.INVITACION_ORIENTACION_RECHAZADA,
                profesorId,
                "INVITACION_ORIENTACION", saved.getId(),
                Map.of("trabajoId", i.getTrabajo().getId()),
                VisibilidadActividad.PARTICIPANTES,
                List.of(profesorId, i.getTrabajo().getEstudiante().getId())));
        return mapper.toResponse(saved);
    }

    @Transactional
    public InvitacionOrientacionResponse cancelar(Long invitacionId, Long estudianteId) {
        var i = repository.findById(invitacionId)
                .orElseThrow(() -> new ResourceNotFoundException("InvitacionOrientacion", invitacionId));
        if (i.getTrabajo().getEstudiante() == null
                || !i.getTrabajo().getEstudiante().getId().equals(estudianteId)) {
            throw new BusinessException("Solo el dueño del trabajo puede cancelar la invitación");
        }
        if (i.getEstado() != EstadoInvitacion.PENDIENTE) {
            throw new BusinessException("La invitación ya fue resuelta");
        }
        i.setEstado(EstadoInvitacion.CANCELADA);
        i.setResueltaEn(Instant.now());
        var saved = repository.save(i);
        events.publishEvent(ActividadEvent.of(
                TipoActividad.INVITACION_ORIENTACION_CANCELADA,
                estudianteId,
                "INVITACION_ORIENTACION", saved.getId(),
                Map.of("trabajoId", i.getTrabajo().getId()),
                VisibilidadActividad.PARTICIPANTES,
                List.of(estudianteId, i.getProfesor().getId())));
        return mapper.toResponse(saved);
    }

    public List<InvitacionOrientacionResponse> listarRecibidasPendientes(Long profesorId) {
        return repository.findByProfesorIdAndEstadoOrderByCreatedAtDesc(profesorId, EstadoInvitacion.PENDIENTE)
                .stream().map(mapper::toResponse).toList();
    }

    public List<InvitacionOrientacionResponse> listarRecibidas(Long profesorId) {
        return repository.findByProfesorIdOrderByCreatedAtDesc(profesorId)
                .stream().map(mapper::toResponse).toList();
    }

    /** Paginado: {@code soloPendientes} → pestaña Pendientes; si no → Histórico (no pendientes). */
    public Page<InvitacionOrientacionResponse> listarRecibidasPaginadas(
            Long profesorId, boolean soloPendientes, Pageable pageable) {
        Page<InvitacionOrientacion> page = soloPendientes
                ? repository.findByProfesorIdAndEstadoOrderByCreatedAtDesc(
                        profesorId, EstadoInvitacion.PENDIENTE, pageable)
                : repository.findByProfesorIdAndEstadoNotOrderByCreatedAtDesc(
                        profesorId, EstadoInvitacion.PENDIENTE, pageable);
        return page.map(mapper::toResponse);
    }

    public List<InvitacionOrientacionResponse> listarPorTrabajo(Long trabajoId) {
        if (!trabajoRepository.existsById(trabajoId)) {
            throw new ResourceNotFoundException("Trabajo", trabajoId);
        }
        return repository.findByTrabajoIdOrderByCreatedAtDesc(trabajoId)
                .stream().map(mapper::toResponse).toList();
    }
}
