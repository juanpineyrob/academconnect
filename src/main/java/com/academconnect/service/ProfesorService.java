package com.academconnect.service;

import com.academconnect.dto.ProfesorRequest;
import com.academconnect.dto.ProfesorResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.ProfesorMapper;
import com.academconnect.repository.ProfesorRepository;
import com.academconnect.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProfesorService {

    private final ProfesorRepository profesorRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProfesorMapper mapper;

    public List<ProfesorResponse> listar() {
        return profesorRepository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public ProfesorResponse buscarPorId(Long id) {
        return profesorRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor", id));
    }

    @Transactional
    public ProfesorResponse crear(ProfesorRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("Ya existe un usuario con el email: " + request.email());
        }
        var profesor = mapper.toEntity(request);
        return mapper.toResponse(profesorRepository.save(profesor));
    }

    @Transactional
    public ProfesorResponse actualizar(Long id, ProfesorRequest request) {
        var profesor = profesorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor", id));
        if (!profesor.getEmail().equals(request.email()) && usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("Ya existe un usuario con el email: " + request.email());
        }
        mapper.update(request, profesor);
        return mapper.toResponse(profesorRepository.save(profesor));
    }

    @Transactional
    public void desactivar(Long id) {
        var profesor = profesorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor", id));
        profesor.setActivo(false);
        profesorRepository.save(profesor);
    }
}
