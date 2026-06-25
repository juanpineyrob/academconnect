package com.academconnect.service;

import com.academconnect.domain.Coorientador;
import com.academconnect.domain.EstadoInvitacion;
import com.academconnect.domain.Rol;
import com.academconnect.domain.SolicitudCoorientacion;
import com.academconnect.domain.Usuario;
import com.academconnect.dto.RespuestaInvitacionRequest;
import com.academconnect.dto.SolicitudCoorientacionRequest;
import com.academconnect.dto.SolicitudCoorientacionResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.SolicitudCoorientacionMapper;
import com.academconnect.repository.CoorientadorRepository;
import com.academconnect.repository.SolicitudCoorientacionRepository;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.repository.UsuarioRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SolicitudCoorientacionService {

    private final SolicitudCoorientacionRepository repository;
    private final TrabajoRepository trabajoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CoorientadorRepository coorientadorRepository;
    private final SolicitudCoorientacionMapper mapper;

    @Transactional
    public SolicitudCoorientacionResponse crear(SolicitudCoorientacionRequest request, Long estudianteId) {
        var trabajo = trabajoRepository.findById(request.trabajoId())
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", request.trabajoId()));
        if (trabajo.getEstudiante() == null || !trabajo.getEstudiante().getId().equals(estudianteId)) {
            throw new BusinessException("No sos el dueño de este trabajo");
        }
        if (trabajo.getOrientador() == null) {
            throw new BusinessException("El trabajo aún no tiene orientador");
        }
        if (!trabajo.getEstado().esActivo()) {
            throw new BusinessException("No se puede solicitar coorientador en un trabajo finalizado");
        }
        if (coorientadorRepository.countByTrabajoId(trabajo.getId()) > 0) {
            throw new BusinessException("El trabajo ya tiene coorientador");
        }
        if (repository.existsByTrabajoIdAndEstado(trabajo.getId(), EstadoInvitacion.PENDIENTE)) {
            throw new BusinessException("Ya hay una solicitud de coorientación pendiente");
        }
        var invitado = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", request.usuarioId()));
        if (!invitado.isActivo()) {
            throw new BusinessException("El usuario no está activo");
        }
        if (invitado.getRol() != Rol.PROFESOR && invitado.getRol() != Rol.EXTERNO) {
            throw new BusinessException("El coorientador debe ser un profesor o un externo");
        }
        if (invitado.getId().equals(trabajo.getOrientador().getId())) {
            throw new BusinessException("El coorientador no puede ser el orientador");
        }
        if (invitado.getId().equals(estudianteId)) {
            throw new BusinessException("No podés invitarte a vos mismo");
        }

        var solicitud = new SolicitudCoorientacion();
        solicitud.setTrabajo(trabajo);
        solicitud.setInvitado(invitado);
        solicitud.setEstado(EstadoInvitacion.PENDIENTE);
        solicitud.setMotivo(request.motivo());
        var saved = repository.save(solicitud);
        return mapper.toResponse(saved);
    }

    @Transactional
    public SolicitudCoorientacionResponse aceptar(
            Long solicitudId, RespuestaInvitacionRequest request, Long usuarioId) {
        var s = repository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("SolicitudCoorientacion", solicitudId));
        if (!s.getInvitado().getId().equals(usuarioId)) {
            throw new BusinessException("Solo el invitado puede aceptar");
        }
        if (s.getEstado() != EstadoInvitacion.PENDIENTE) {
            throw new BusinessException("La solicitud ya fue resuelta");
        }
        var trabajo = s.getTrabajo();
        if (trabajo.getOrientador() == null || !trabajo.getEstado().esActivo()
                || coorientadorRepository.countByTrabajoId(trabajo.getId()) > 0) {
            throw new BusinessException("La solicitud ya no es válida para este trabajo");
        }

        s.setEstado(EstadoInvitacion.ACEPTADA);
        s.setRespuesta(request != null ? request.respuesta() : null);
        s.setResueltaEn(Instant.now());

        var coorientador = new Coorientador();
        coorientador.setTrabajo(trabajo);
        coorientador.setUsuario(s.getInvitado());
        coorientador.setDesde(LocalDate.now());
        coorientadorRepository.save(coorientador);

        return mapper.toResponse(repository.save(s));
    }

    @Transactional
    public SolicitudCoorientacionResponse rechazar(
            Long solicitudId, RespuestaInvitacionRequest request, Long usuarioId) {
        var s = repository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("SolicitudCoorientacion", solicitudId));
        if (!s.getInvitado().getId().equals(usuarioId)) {
            throw new BusinessException("Solo el invitado puede rechazar");
        }
        if (s.getEstado() != EstadoInvitacion.PENDIENTE) {
            throw new BusinessException("La solicitud ya fue resuelta");
        }
        s.setEstado(EstadoInvitacion.RECHAZADA);
        s.setRespuesta(request != null ? request.respuesta() : null);
        s.setResueltaEn(Instant.now());
        return mapper.toResponse(repository.save(s));
    }

    @Transactional
    public SolicitudCoorientacionResponse cancelar(Long solicitudId, Long estudianteId) {
        var s = repository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("SolicitudCoorientacion", solicitudId));
        if (s.getTrabajo().getEstudiante() == null
                || !s.getTrabajo().getEstudiante().getId().equals(estudianteId)) {
            throw new BusinessException("Solo el dueño puede cancelar");
        }
        if (s.getEstado() != EstadoInvitacion.PENDIENTE) {
            throw new BusinessException("La solicitud ya fue resuelta");
        }
        s.setEstado(EstadoInvitacion.CANCELADA);
        s.setResueltaEn(Instant.now());
        return mapper.toResponse(repository.save(s));
    }

    public Page<SolicitudCoorientacionResponse> listarRecibidasPaginadas(
            Long usuarioId, boolean soloPendientes, Pageable pageable) {
        Page<SolicitudCoorientacion> page = soloPendientes
                ? repository.findByInvitadoIdAndEstadoOrderByCreatedAtDesc(
                        usuarioId, EstadoInvitacion.PENDIENTE, pageable)
                : repository.findByInvitadoIdAndEstadoNotOrderByCreatedAtDesc(
                        usuarioId, EstadoInvitacion.PENDIENTE, pageable);
        return page.map(mapper::toResponse);
    }

    public List<SolicitudCoorientacionResponse> listarPorTrabajo(Long trabajoId) {
        if (!trabajoRepository.existsById(trabajoId)) {
            throw new ResourceNotFoundException("Trabajo", trabajoId);
        }
        return repository.findByTrabajoIdOrderByCreatedAtDesc(trabajoId)
                .stream().map(mapper::toResponse).toList();
    }
}
