package com.academconnect.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.Trabajo;
import com.academconnect.domain.Versionamiento;
import com.academconnect.domain.VisibilidadActividad;
import com.academconnect.dto.VersionamientoResponse;
import com.academconnect.event.ActividadEvent;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.VersionamientoMapper;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.repository.VersionamientoRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VersionamientoService {

    private static final int MAX_ACTIVAS_POR_TRABAJO = 10;

    private final VersionamientoRepository versionamientoRepository;
    private final TrabajoRepository trabajoRepository;
    private final DocumentoService documentoService;
    private final VersionamientoMapper mapper;
    private final ApplicationEventPublisher events;

    public List<VersionamientoResponse> listarPorTrabajo(Long trabajoId, boolean includeDeleted) {
        if (!trabajoRepository.existsById(trabajoId)) {
            throw new ResourceNotFoundException("Trabajo", trabajoId);
        }
        var lista = includeDeleted
                ? versionamientoRepository.findByTrabajoIdOrderByNumeroVersionDesc(trabajoId)
                : versionamientoRepository.findByTrabajoIdAndDeletedAtIsNullOrderByNumeroVersionDesc(trabajoId);
        return lista.stream().map(mapper::toResponse).toList();
    }

    public VersionamientoResponse buscarPorId(Long id) {
        return versionamientoRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Versionamiento", id));
    }

    @Transactional
    public VersionamientoResponse crear(Long trabajoId, MultipartFile file, String comentario) {
        var trabajo = trabajoRepository.findById(trabajoId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", trabajoId));

        long activas = versionamientoRepository.countByTrabajoIdAndDeletedAtIsNull(trabajoId);
        if (activas >= MAX_ACTIVAS_POR_TRABAJO) {
            throw new BusinessException("Máximo " + MAX_ACTIVAS_POR_TRABAJO + " entregas activas por trabajo");
        }

        var documento = documentoService.guardarSiNuevo(file);
        int siguienteVersion = siguienteNumeroVersion(trabajoId);

        var version = new Versionamiento();
        version.setTrabajo(trabajo);
        version.setDocumento(documento);
        version.setNumeroVersion(siguienteVersion);
        version.setComentario(comentario);
        var saved = versionamientoRepository.save(version);

        publicarActividad(TipoActividad.VERSION_SUBIDA, trabajo, saved.getId(),
                Map.of("trabajoId", trabajo.getId(),
                        "trabajoTitulo", trabajo.getTitulo(),
                        "numeroVersion", siguienteVersion));

        return mapper.toResponse(saved);
    }

    @Transactional
    public VersionamientoResponse reemplazar(Long trabajoId, Long versionId, MultipartFile file, String comentario, Long callerEstudianteId) {
        var trabajo = trabajoRepository.findById(trabajoId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", trabajoId));
        if (trabajo.getEstudiante() == null || !trabajo.getEstudiante().getId().equals(callerEstudianteId)) {
            throw new BusinessException("Solo el estudiante dueño puede reemplazar entregas");
        }

        var vieja = versionamientoRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Versionamiento", versionId));
        if (!vieja.getTrabajo().getId().equals(trabajoId)) {
            throw new ResourceNotFoundException("Versionamiento", versionId);
        }
        if (!vieja.isActiva()) {
            throw new ResourceNotFoundException("Versionamiento", versionId);
        }

        // delta neto del reemplazo es 0: tope se mide como > MAX (no debería pasar con la guarda en crear)
        long activas = versionamientoRepository.countByTrabajoIdAndDeletedAtIsNull(trabajoId);
        if (activas > MAX_ACTIVAS_POR_TRABAJO) {
            throw new BusinessException("Máximo " + MAX_ACTIVAS_POR_TRABAJO + " entregas activas por trabajo");
        }

        Instant now = Instant.now();
        vieja.setDeletedAt(now);
        vieja.setDeletedBy(actorLabel(callerEstudianteId));
        versionamientoRepository.save(vieja);

        var documento = documentoService.guardarSiNuevo(file);
        int siguienteVersion = siguienteNumeroVersion(trabajoId);
        var nueva = new Versionamiento();
        nueva.setTrabajo(trabajo);
        nueva.setDocumento(documento);
        nueva.setNumeroVersion(siguienteVersion);
        nueva.setComentario(comentario);
        var saved = versionamientoRepository.save(nueva);

        publicarActividad(TipoActividad.VERSION_REEMPLAZADA, trabajo, saved.getId(),
                Map.of("trabajoId", trabajo.getId(),
                        "trabajoTitulo", trabajo.getTitulo(),
                        "oldVersionId", versionId,
                        "newVersionId", saved.getId(),
                        "numeroVersion", siguienteVersion));

        return mapper.toResponse(saved);
    }

    @Transactional
    public void eliminar(Long trabajoId, Long versionId, Long callerEstudianteId) {
        var trabajo = trabajoRepository.findById(trabajoId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", trabajoId));
        if (trabajo.getEstudiante() == null || !trabajo.getEstudiante().getId().equals(callerEstudianteId)) {
            throw new BusinessException("Solo el estudiante dueño puede eliminar entregas");
        }

        var version = versionamientoRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Versionamiento", versionId));
        if (!version.getTrabajo().getId().equals(trabajoId) || !version.isActiva()) {
            throw new ResourceNotFoundException("Versionamiento", versionId);
        }

        version.setDeletedAt(Instant.now());
        version.setDeletedBy(actorLabel(callerEstudianteId));
        versionamientoRepository.save(version);

        publicarActividad(TipoActividad.VERSION_ELIMINADA, trabajo, version.getId(),
                Map.of("trabajoId", trabajo.getId(),
                        "trabajoTitulo", trabajo.getTitulo(),
                        "versionId", version.getId(),
                        "numeroVersion", version.getNumeroVersion()));
    }

    private int siguienteNumeroVersion(Long trabajoId) {
        return versionamientoRepository
                .findFirstByTrabajoIdOrderByNumeroVersionDesc(trabajoId)
                .map(v -> v.getNumeroVersion() + 1)
                .orElse(1);
    }

    private String actorLabel(Long usuarioId) {
        return "usuario:" + usuarioId;
    }

    private void publicarActividad(TipoActividad tipo, Trabajo trabajo, Long entidadId, Map<String, Object> data) {
        List<Long> participantes = new ArrayList<>();
        Long actorId = null;
        if (trabajo.getEstudiante() != null) {
            participantes.add(trabajo.getEstudiante().getId());
            actorId = trabajo.getEstudiante().getId();
        }
        if (trabajo.getOrientador() != null) participantes.add(trabajo.getOrientador().getId());
        events.publishEvent(ActividadEvent.of(
                tipo,
                actorId,
                "VERSIONAMIENTO", entidadId,
                data,
                VisibilidadActividad.PARTICIPANTES,
                participantes));
    }
}
