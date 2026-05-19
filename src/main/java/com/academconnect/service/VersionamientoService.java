package com.academconnect.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.academconnect.domain.Versionamiento;
import com.academconnect.dto.VersionamientoResponse;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.VersionamientoMapper;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.repository.VersionamientoRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VersionamientoService {

    private final VersionamientoRepository versionamientoRepository;
    private final TrabajoRepository trabajoRepository;
    private final DocumentoService documentoService;
    private final VersionamientoMapper mapper;

    public List<VersionamientoResponse> listarPorTrabajo(Long trabajoId) {
        if (!trabajoRepository.existsById(trabajoId)) {
            throw new ResourceNotFoundException("Trabajo", trabajoId);
        }
        return versionamientoRepository.findByTrabajoIdOrderByNumeroVersionDesc(trabajoId).stream()
                .map(mapper::toResponse)
                .toList();
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

        var documento = documentoService.guardarSiNuevo(file);

        int siguienteVersion = versionamientoRepository
                .findFirstByTrabajoIdOrderByNumeroVersionDesc(trabajoId)
                .map(v -> v.getNumeroVersion() + 1)
                .orElse(1);

        var version = new Versionamiento();
        version.setTrabajo(trabajo);
        version.setDocumento(documento);
        version.setNumeroVersion(siguienteVersion);
        version.setComentario(comentario);

        return mapper.toResponse(versionamientoRepository.save(version));
    }
}
