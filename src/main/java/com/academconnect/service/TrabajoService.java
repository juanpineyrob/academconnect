package com.academconnect.service;

import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.dto.TrabajoRequest;
import com.academconnect.dto.TrabajoResponse;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.TrabajoMapper;
import com.academconnect.repository.AreaTematicaRepository;
import com.academconnect.repository.ProfesorRepository;
import com.academconnect.repository.TrabajoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TrabajoService {

    private final TrabajoRepository trabajoRepository;
    private final ProfesorRepository profesorRepository;
    private final AreaTematicaRepository areaTematicaRepository;
    private final TrabajoMapper mapper;

    public List<TrabajoResponse> listar() {
        return trabajoRepository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<TrabajoResponse> listarPorEstado(EstadoTrabajo estado) {
        return trabajoRepository.findByEstado(estado).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public TrabajoResponse buscarPorId(Long id) {
        return trabajoRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", id));
    }

    @Transactional
    public TrabajoResponse crear(TrabajoRequest request) {
        var orientador = profesorRepository.findById(request.orientadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesor", request.orientadorId()));

        var trabajo = mapper.toEntity(request);
        trabajo.setOrientador(orientador);
        trabajo.setEstado(EstadoTrabajo.BORRADOR);

        if (request.areaIds() != null && !request.areaIds().isEmpty()) {
            Set<AreaTematica> areas = new HashSet<>(areaTematicaRepository.findAllById(request.areaIds()));
            trabajo.setAreas(areas);
        }

        return mapper.toResponse(trabajoRepository.save(trabajo));
    }

    @Transactional
    public TrabajoResponse actualizar(Long id, TrabajoRequest request) {
        var trabajo = trabajoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", id));

        var orientador = profesorRepository.findById(request.orientadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesor", request.orientadorId()));

        mapper.update(request, trabajo);
        trabajo.setOrientador(orientador);

        Set<AreaTematica> areas = (request.areaIds() != null && !request.areaIds().isEmpty())
                ? new HashSet<>(areaTematicaRepository.findAllById(request.areaIds()))
                : new HashSet<>();
        trabajo.setAreas(areas);

        return mapper.toResponse(trabajoRepository.save(trabajo));
    }
}
