package com.academconnect.service;

import com.academconnect.domain.AreaTematica;
import com.academconnect.dto.AreaTematicaRequest;
import com.academconnect.dto.AreaTematicaResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.AreaTematicaMapper;
import com.academconnect.repository.AreaTematicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AreaTematicaService {

    private final AreaTematicaRepository repository;
    private final AreaTematicaMapper mapper;

    public List<AreaTematicaResponse> listarActivas() {
        return repository.findAll().stream()
                .filter(a -> a.isActivo())
                .map(mapper::toResponse)
                .toList();
    }

    public AreaTematicaResponse buscarPorId(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("AreaTematica", id));
    }

    /** Administración: áreas (incluidas inactivas) paginadas y filtradas por texto. */
    public Page<AreaTematicaResponse> buscar(String q, Pageable pageable) {
        String patron = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        return repository.buscarAdmin(patron, pageable).map(mapper::toResponse);
    }

    @Transactional
    public AreaTematicaResponse crear(AreaTematicaRequest request) {
        var area = new AreaTematica();
        aplicar(area, request);
        return mapper.toResponse(repository.save(area));
    }

    @Transactional
    public AreaTematicaResponse actualizar(Long id, AreaTematicaRequest request) {
        var area = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AreaTematica", id));
        aplicar(area, request);
        return mapper.toResponse(repository.save(area));
    }

    /** Borrado lógico/reactivación: las áreas se referencian desde trabajos y perfiles (FK RESTRICT). */
    @Transactional
    public AreaTematicaResponse setActivo(Long id, boolean activo) {
        var area = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AreaTematica", id));
        area.setActivo(activo);
        return mapper.toResponse(repository.save(area));
    }

    private void aplicar(AreaTematica area, AreaTematicaRequest request) {
        area.setNombre(request.nombre().trim());
        var codigo = request.codigoExterno();
        area.setCodigoExterno(codigo == null || codigo.isBlank() ? null : codigo.trim());
        area.setThesaurusOrigen(request.thesaurusOrigen());
        if (request.parentId() != null) {
            if (request.parentId().equals(area.getId())) {
                throw new BusinessException("Un área no puede ser su propio padre");
            }
            var parent = repository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("AreaTematica padre", request.parentId()));
            area.setParent(parent);
        } else {
            area.setParent(null);
        }
    }
}
