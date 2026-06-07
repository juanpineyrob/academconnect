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
}
