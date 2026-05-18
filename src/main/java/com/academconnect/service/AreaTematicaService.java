package com.academconnect.service;

import com.academconnect.dto.AreaTematicaResponse;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.AreaTematicaMapper;
import com.academconnect.repository.AreaTematicaRepository;
import lombok.RequiredArgsConstructor;
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
}
