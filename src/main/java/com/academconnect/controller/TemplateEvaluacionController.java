package com.academconnect.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.dto.TemplateEvaluacionRequest;
import com.academconnect.dto.TemplateEvaluacionResponse;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.service.TemplateEvaluacionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateEvaluacionController {

    private final TemplateEvaluacionService service;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<TemplateEvaluacionResponse> listar(Authentication authn) {
        return service.listarVisibles(callerId(authn), isAdmin(authn));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public TemplateEvaluacionResponse buscarPorId(@PathVariable Long id, Authentication authn) {
        return service.buscarVisible(id, callerId(authn), isAdmin(authn));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PROFESOR','EXTERNO','ADMINISTRADOR')")
    public TemplateEvaluacionResponse crear(@Valid @RequestBody TemplateEvaluacionRequest request, Authentication authn) {
        return service.crear(request, callerId(authn));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public TemplateEvaluacionResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody TemplateEvaluacionRequest request,
            Authentication authn) {
        return service.actualizar(id, request, callerId(authn), isAdmin(authn));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void desactivar(@PathVariable Long id, Authentication authn) {
        service.desactivar(id, callerId(authn), isAdmin(authn));
    }

    private Long callerId(Authentication authn) {
        return usuarioRepository.findByEmail(authn.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", authn.getName()))
                .getId();
    }

    private boolean isAdmin(Authentication authn) {
        return authn.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }
}
