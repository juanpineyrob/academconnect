package com.academconnect.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.dto.RespuestaSolicitudRequest;
import com.academconnect.dto.SolicitudVinculacionRequest;
import com.academconnect.dto.SolicitudVinculacionResponse;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.service.SolicitudVinculacionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudVinculacionController {

    private final SolicitudVinculacionService service;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public SolicitudVinculacionResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public SolicitudVinculacionResponse crear(@Valid @RequestBody SolicitudVinculacionRequest request) {
        return service.crear(request);
    }

    @PostMapping("/{id}/aceptar")
    @PreAuthorize("hasRole('PROFESOR')")
    public SolicitudVinculacionResponse aceptar(
            @PathVariable Long id,
            @RequestBody(required = false) RespuestaSolicitudRequest request) {
        return service.aceptar(id, request);
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('PROFESOR')")
    public SolicitudVinculacionResponse rechazar(
            @PathVariable Long id,
            @RequestBody(required = false) RespuestaSolicitudRequest request) {
        return service.rechazar(id, request);
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public SolicitudVinculacionResponse cancelar(@PathVariable Long id,
                                                 org.springframework.security.core.Authentication authn) {
        return service.cancelar(id, currentUserId(authn));
    }

    private Long currentUserId(org.springframework.security.core.Authentication authn) {
        var email = authn.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email))
                .getId();
    }
}
