package com.academconnect.service;

import com.academconnect.dto.EstudianteRequest;
import com.academconnect.dto.EstudianteResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.EstudianteMapper;
import com.academconnect.repository.EstudianteRepository;
import com.academconnect.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstudianteMapper mapper;

    public List<EstudianteResponse> listar() {
        return estudianteRepository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public EstudianteResponse buscarPorId(Long id) {
        return estudianteRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", id));
    }

    @Transactional
    public EstudianteResponse crear(EstudianteRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("Ya existe un usuario con el email: " + request.email());
        }
        var estudiante = mapper.toEntity(request);
        return mapper.toResponse(estudianteRepository.save(estudiante));
    }

    @Transactional
    public EstudianteResponse actualizar(Long id, EstudianteRequest request) {
        var estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", id));
        if (!estudiante.getEmail().equals(request.email()) && usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("Ya existe un usuario con el email: " + request.email());
        }
        mapper.update(request, estudiante);
        return mapper.toResponse(estudianteRepository.save(estudiante));
    }

    @Transactional
    public void desactivar(Long id) {
        var estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", id));
        estudiante.setActivo(false);
        estudianteRepository.save(estudiante);
    }
}
