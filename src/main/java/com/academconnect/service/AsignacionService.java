package com.academconnect.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.Asignacion;
import com.academconnect.domain.EstadoAsignacion;
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

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AsignacionService {

    private final AsignacionRepository asignacionRepository;
    private final TrabajoRepository trabajoRepository;
    private final VersionamientoRepository versionamientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TemplateEvaluacionRepository templateRepository;
    private final ConflictoInteresRepository conflictoRepository;
    private final AsignacionMapper mapper;

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
        asignacion.setTemplateSnapshot(template.getCriterios());
        asignacion.setAsignadaEn(Instant.now());
        asignacion.setVencimientoEn(request.vencimientoEn());
        asignacion.setEstado(EstadoAsignacion.ACTIVA);

        return mapper.toResponse(asignacionRepository.save(asignacion));
    }

    public List<AsignacionResponse> listarMisAsignaciones(String email) {
        var evaluador = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email));
        return asignacionRepository.findByEvaluadorIdAndEstado(evaluador.getId(), EstadoAsignacion.ACTIVA)
                .stream()
                .map(mapper::toResponse)
                .toList();
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
