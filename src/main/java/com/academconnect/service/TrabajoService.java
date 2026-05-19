package com.academconnect.service;

import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.dto.TrabajoRequest;
import com.academconnect.dto.TrabajoResponse;
import com.academconnect.exception.BusinessException;
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

    public List<TrabajoResponse> buscarPorTexto(String q) {
        return trabajoRepository.buscarPorTexto(q).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public TrabajoResponse aprobar(Long id) {
        return cambiarEstado(id, EstadoTrabajo.EN_EVALUACION, EstadoTrabajo.APROBADO);
    }

    @Transactional
    public TrabajoResponse rechazar(Long id) {
        return cambiarEstado(id, EstadoTrabajo.EN_EVALUACION, EstadoTrabajo.RECHAZADO);
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

    private TrabajoResponse cambiarEstado(Long id, EstadoTrabajo estadoRequerido, EstadoTrabajo nuevoEstado) {
        var trabajo = trabajoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", id));
        if (trabajo.getEstado() != estadoRequerido) {
            throw new BusinessException(
                    "El trabajo debe estar en estado %s para pasar a %s".formatted(estadoRequerido, nuevoEstado));
        }
        trabajo.setEstado(nuevoEstado);
        return mapper.toResponse(trabajoRepository.save(trabajo));
    }
}
