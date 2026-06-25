package com.academconnect.service;

import com.academconnect.domain.EstadoAsignacion;
import com.academconnect.domain.EstadoInvitacion;
import com.academconnect.domain.Rol;
import com.academconnect.domain.SolicitudEvaluacion;
import com.academconnect.domain.Usuario;
import com.academconnect.dto.AsignacionRequest;
import com.academconnect.dto.RespuestaInvitacionRequest;
import com.academconnect.dto.SolicitudEvaluacionRequest;
import com.academconnect.dto.SolicitudEvaluacionResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.SolicitudEvaluacionMapper;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.CoorientadorRepository;
import com.academconnect.repository.ConflictoInteresRepository;
import com.academconnect.repository.SolicitudEvaluacionRepository;
import com.academconnect.repository.TemplateEvaluacionRepository;
import com.academconnect.repository.TipoTrabajoConfigRepository;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.repository.VersionamientoRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SolicitudEvaluacionService {

    private final SolicitudEvaluacionRepository repository;
    private final TrabajoRepository trabajoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsignacionRepository asignacionRepository;
    private final VersionamientoRepository versionamientoRepository;
    private final TipoTrabajoConfigRepository tipoTrabajoConfigRepository;
    private final TemplateEvaluacionRepository templateRepository;
    private final CoorientadorRepository coorientadorRepository;
    private final ConflictoInteresRepository conflictoRepository;
    private final AsignacionService asignacionService;
    private final SolicitudEvaluacionMapper mapper;
    private final InstanciaEvaluacionService instanciaEvaluacionService;

    private int evaluadoresRequeridos(com.academconnect.domain.Trabajo trabajo) {
        var activa = instanciaEvaluacionService.instanciaActiva(trabajo.getId());
        if (activa.isPresent()) {
            return activa.get().getInstanciaConfig().getEvaluadoresRequeridos();
        }
        return tipoTrabajoConfigRepository.findById(trabajo.getTipo())
                .orElseThrow(() -> new BusinessException(
                        "No hay configuración de evaluadores para el tipo " + trabajo.getTipo()))
                .getEvaluadoresDefault();
    }

    private long bancaOcupada(Long trabajoId) {
        return asignacionRepository.countByTrabajoIdAndEstado(trabajoId, EstadoAsignacion.ACTIVA)
                + repository.countByTrabajoIdAndEstado(trabajoId, EstadoInvitacion.PENDIENTE);
    }

    @Transactional
    public SolicitudEvaluacionResponse crear(SolicitudEvaluacionRequest request, Long estudianteId) {
        var trabajo = trabajoRepository.findById(request.trabajoId())
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", request.trabajoId()));
        if (trabajo.getEstudiante() == null || !trabajo.getEstudiante().getId().equals(estudianteId)) {
            throw new BusinessException("No sos el dueño de este trabajo");
        }
        if (trabajo.getOrientador() == null) {
            throw new BusinessException("El trabajo aún no tiene orientador");
        }
        if (!trabajo.getEstado().esActivo()) {
            throw new BusinessException("No se puede solicitar evaluadores en un trabajo finalizado");
        }
        if (versionamientoRepository.findFirstByTrabajoIdOrderByNumeroVersionDesc(trabajo.getId()).isEmpty()) {
            throw new BusinessException("El trabajo no tiene ninguna versión para evaluar");
        }
        int n = evaluadoresRequeridos(trabajo);
        if (bancaOcupada(trabajo.getId()) >= n) {
            throw new BusinessException("La banca evaluadora ya está completa");
        }
        var invitado = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", request.usuarioId()));
        if (!invitado.isActivo()) {
            throw new BusinessException("El usuario no está activo");
        }
        if (invitado.getRol() != Rol.PROFESOR && invitado.getRol() != Rol.EXTERNO) {
            throw new BusinessException("El evaluador debe ser un profesor o un externo");
        }
        if (invitado.getId().equals(trabajo.getOrientador().getId())) {
            throw new BusinessException("El evaluador no puede ser el orientador");
        }
        if (invitado.getId().equals(estudianteId)) {
            throw new BusinessException("No podés invitarte a vos mismo");
        }
        boolean esCoorientador = coorientadorRepository.findByTrabajoId(trabajo.getId()).stream()
                .anyMatch(c -> c.getUsuario().getId().equals(invitado.getId()));
        if (esCoorientador) {
            throw new BusinessException("El coorientador no puede ser evaluador");
        }
        if (conflictoRepository.existsByTrabajoIdAndEvaluadorId(trabajo.getId(), invitado.getId())) {
            throw new BusinessException("El evaluador tiene conflicto de interés con este trabajo");
        }
        if (repository.existsByTrabajoIdAndInvitadoIdAndEstado(
                trabajo.getId(), invitado.getId(), EstadoInvitacion.PENDIENTE)) {
            throw new BusinessException("Ya hay una solicitud pendiente para este evaluador");
        }
        if (asignacionRepository.findByTrabajoId(trabajo.getId()).stream()
                .anyMatch(a -> a.getEvaluador().getId().equals(invitado.getId())
                        && a.getEstado() == EstadoAsignacion.ACTIVA)) {
            throw new BusinessException("Este evaluador ya está asignado al trabajo");
        }

        var solicitud = new SolicitudEvaluacion();
        solicitud.setTrabajo(trabajo);
        solicitud.setInvitado(invitado);
        solicitud.setEstado(EstadoInvitacion.PENDIENTE);
        solicitud.setMotivo(request.motivo());
        return mapper.toResponse(repository.save(solicitud));
    }

    @Transactional
    public SolicitudEvaluacionResponse aceptar(
            Long solicitudId, RespuestaInvitacionRequest request, Long usuarioId) {
        var s = repository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("SolicitudEvaluacion", solicitudId));
        if (!s.getInvitado().getId().equals(usuarioId)) {
            throw new BusinessException("Solo el evaluador invitado puede aceptar");
        }
        if (s.getEstado() != EstadoInvitacion.PENDIENTE) {
            throw new BusinessException("La solicitud ya fue resuelta");
        }
        var trabajo = s.getTrabajo();
        if (trabajo.getOrientador() == null || !trabajo.getEstado().esActivo()) {
            throw new BusinessException("La solicitud ya no es válida para este trabajo");
        }
        int n = evaluadoresRequeridos(trabajo);
        if (asignacionRepository.countByTrabajoIdAndEstado(trabajo.getId(), EstadoAsignacion.ACTIVA) >= n) {
            throw new BusinessException("La banca evaluadora ya está completa");
        }
        var version = versionamientoRepository.findFirstByTrabajoIdOrderByNumeroVersionDesc(trabajo.getId())
                .orElseThrow(() -> new BusinessException("El trabajo no tiene ninguna versión para evaluar"));
        var template = templateRepository.findFirstByEsPorDefectoTrueAndActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un template de evaluación por defecto configurado"));

        var resp = asignacionService.crear(new AsignacionRequest(
                trabajo.getId(), version.getId(), s.getInvitado().getId(), template.getId(), null));
        instanciaEvaluacionService.instanciaActiva(trabajo.getId()).ifPresent(ie -> {
            asignacionRepository.findById(resp.id()).ifPresent(a -> {
                a.setInstanciaEvaluacion(ie);
                asignacionRepository.save(a);
            });
            instanciaEvaluacionService.marcarEnCurso(ie);
        });

        s.setEstado(EstadoInvitacion.ACEPTADA);
        s.setRespuesta(request != null ? request.respuesta() : null);
        s.setResueltaEn(Instant.now());
        return mapper.toResponse(repository.save(s));
    }

    @Transactional
    public SolicitudEvaluacionResponse rechazar(
            Long solicitudId, RespuestaInvitacionRequest request, Long usuarioId) {
        var s = repository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("SolicitudEvaluacion", solicitudId));
        if (!s.getInvitado().getId().equals(usuarioId)) {
            throw new BusinessException("Solo el evaluador invitado puede rechazar");
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
    public SolicitudEvaluacionResponse cancelar(Long solicitudId, Long estudianteId) {
        var s = repository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("SolicitudEvaluacion", solicitudId));
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

    public Page<SolicitudEvaluacionResponse> listarRecibidasPaginadas(
            Long usuarioId, boolean soloPendientes, Pageable pageable) {
        Page<SolicitudEvaluacion> page = soloPendientes
                ? repository.findByInvitadoIdAndEstadoOrderByCreatedAtDesc(
                        usuarioId, EstadoInvitacion.PENDIENTE, pageable)
                : repository.findByInvitadoIdAndEstadoNotOrderByCreatedAtDesc(
                        usuarioId, EstadoInvitacion.PENDIENTE, pageable);
        return page.map(mapper::toResponse);
    }

    public List<SolicitudEvaluacionResponse> listarPorTrabajo(Long trabajoId) {
        if (!trabajoRepository.existsById(trabajoId)) {
            throw new ResourceNotFoundException("Trabajo", trabajoId);
        }
        return repository.findByTrabajoIdOrderByCreatedAtDesc(trabajoId)
                .stream().map(mapper::toResponse).toList();
    }
}
