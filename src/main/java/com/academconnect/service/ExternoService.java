package com.academconnect.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.dto.ExternoRequest;
import com.academconnect.dto.ExternoResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.ExternoMapper;
import com.academconnect.repository.ExternoRepository;
import com.academconnect.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExternoService {

    private final ExternoRepository externoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ExternoMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public List<ExternoResponse> listar() {
        return externoRepository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public ExternoResponse buscarPorId(Long id) {
        return externoRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Externo", id));
    }

    @Transactional
    public ExternoResponse crear(ExternoRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("Ya existe un usuario con el email: " + request.email());
        }
        var externo = mapper.toEntity(request);
        externo.setPassword(passwordEncoder.encode(request.password()));
        return mapper.toResponse(externoRepository.save(externo));
    }

    @Transactional
    public ExternoResponse actualizar(Long id, ExternoRequest request) {
        var externo = externoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Externo", id));
        if (!externo.getEmail().equals(request.email()) && usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("Ya existe un usuario con el email: " + request.email());
        }
        mapper.update(request, externo);
        externo.setPassword(passwordEncoder.encode(request.password()));
        return mapper.toResponse(externoRepository.save(externo));
    }

    @Transactional
    public void desactivar(Long id) {
        var externo = externoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Externo", id));
        externo.setActivo(false);
        externoRepository.save(externo);
    }
}
