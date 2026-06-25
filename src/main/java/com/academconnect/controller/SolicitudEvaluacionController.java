package com.academconnect.controller;

import com.academconnect.domain.EstadoInvitacion;
import com.academconnect.dto.RespuestaInvitacionRequest;
import com.academconnect.dto.SolicitudEvaluacionRequest;
import com.academconnect.dto.SolicitudEvaluacionResponse;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.service.SolicitudEvaluacionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes-evaluacion")
@RequiredArgsConstructor
public class SolicitudEvaluacionController {

    private final SolicitudEvaluacionService service;
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public SolicitudEvaluacionResponse crear(
            @Valid @RequestBody SolicitudEvaluacionRequest request, Authentication authn) {
        return service.crear(request, currentUserId(authn));
    }

    @PostMapping("/{id}/aceptar")
    @PreAuthorize("hasRole('PROFESOR') or hasRole('EXTERNO')")
    public SolicitudEvaluacionResponse aceptar(
            @PathVariable Long id,
            @RequestBody(required = false) RespuestaInvitacionRequest request,
            Authentication authn) {
        return service.aceptar(id, request, currentUserId(authn));
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('PROFESOR') or hasRole('EXTERNO')")
    public SolicitudEvaluacionResponse rechazar(
            @PathVariable Long id,
            @RequestBody(required = false) RespuestaInvitacionRequest request,
            Authentication authn) {
        return service.rechazar(id, request, currentUserId(authn));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public SolicitudEvaluacionResponse cancelar(@PathVariable Long id, Authentication authn) {
        return service.cancelar(id, currentUserId(authn));
    }

    @GetMapping
    @PreAuthorize("hasRole('PROFESOR') or hasRole('EXTERNO')")
    public Page<SolicitudEvaluacionResponse> recibidas(
            @RequestParam(required = false) EstadoInvitacion estado,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authn) {
        return service.listarRecibidasPaginadas(
                currentUserId(authn), estado == EstadoInvitacion.PENDIENTE, pageable);
    }

    @GetMapping("/trabajos/{trabajoId}")
    @PreAuthorize("isAuthenticated()")
    public List<SolicitudEvaluacionResponse> porTrabajo(@PathVariable Long trabajoId) {
        return service.listarPorTrabajo(trabajoId);
    }

    private Long currentUserId(Authentication authn) {
        var email = authn.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email))
                .getId();
    }
}
