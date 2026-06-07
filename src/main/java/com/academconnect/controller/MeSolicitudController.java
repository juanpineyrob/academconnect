package com.academconnect.controller;

import com.academconnect.dto.SolicitudVinculacionResponse;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.SolicitudVinculacionMapper;
import com.academconnect.repository.SolicitudVinculacionRepository;
import com.academconnect.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeSolicitudController {

    private final SolicitudVinculacionRepository solicitudRepository;
    private final SolicitudVinculacionMapper mapper;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/solicitudes")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public List<SolicitudVinculacionResponse> misSolicitudes(Authentication authn) {
        Long me = currentUserId(authn);
        return solicitudRepository.findByEstudianteIdOrderByCreatedAtDesc(me).stream()
                .map(mapper::toResponse).toList();
    }

    @GetMapping("/solicitudes-recibidas")
    @PreAuthorize("hasRole('PROFESOR')")
    public List<SolicitudVinculacionResponse> recibidas(Authentication authn) {
        Long me = currentUserId(authn);
        return solicitudRepository.findRecibidasPorProfesor(me).stream()
                .map(mapper::toResponse).toList();
    }

    private Long currentUserId(Authentication authn) {
        var email = authn.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email))
                .getId();
    }
}
