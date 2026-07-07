package com.academconnect.controller;

import com.academconnect.dto.SolicitudVinculacionResponse;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.service.SolicitudVinculacionService;
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

    private final SolicitudVinculacionService solicitudService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/solicitudes")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public List<SolicitudVinculacionResponse> misSolicitudes(Authentication authn) {
        return solicitudService.listarPorEstudiante(currentUserId(authn));
    }

    @GetMapping("/solicitudes-recibidas")
    @PreAuthorize("hasRole('PROFESOR')")
    public List<SolicitudVinculacionResponse> recibidas(Authentication authn) {
        return solicitudService.listarRecibidasPorProfesor(currentUserId(authn));
    }

    private Long currentUserId(Authentication authn) {
        var email = authn.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email))
                .getId();
    }
}
