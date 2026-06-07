package com.academconnect.controller;

import com.academconnect.domain.EstadoInvitacion;
import com.academconnect.dto.InvitacionOrientacionRequest;
import com.academconnect.dto.InvitacionOrientacionResponse;
import com.academconnect.dto.RespuestaInvitacionRequest;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.service.InvitacionOrientacionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/invitaciones-orientacion")
@RequiredArgsConstructor
public class InvitacionOrientacionController {

    private final InvitacionOrientacionService service;
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public InvitacionOrientacionResponse crear(
            @Valid @RequestBody InvitacionOrientacionRequest request, Authentication authn) {
        return service.crear(request, currentUserId(authn));
    }

    @PostMapping("/{id}/aceptar")
    @PreAuthorize("hasRole('PROFESOR')")
    public InvitacionOrientacionResponse aceptar(
            @PathVariable Long id,
            @RequestBody(required = false) RespuestaInvitacionRequest request,
            Authentication authn) {
        return service.aceptar(id, request, currentUserId(authn));
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('PROFESOR')")
    public InvitacionOrientacionResponse rechazar(
            @PathVariable Long id,
            @RequestBody(required = false) RespuestaInvitacionRequest request,
            Authentication authn) {
        return service.rechazar(id, request, currentUserId(authn));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public InvitacionOrientacionResponse cancelar(@PathVariable Long id, Authentication authn) {
        return service.cancelar(id, currentUserId(authn));
    }

    @GetMapping
    @PreAuthorize("hasRole('PROFESOR')")
    public List<InvitacionOrientacionResponse> recibidas(
            @RequestParam(required = false) EstadoInvitacion estado, Authentication authn) {
        var id = currentUserId(authn);
        return estado == EstadoInvitacion.PENDIENTE
                ? service.listarRecibidasPendientes(id)
                : service.listarRecibidas(id);
    }

    private Long currentUserId(Authentication authn) {
        var email = authn.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email))
                .getId();
    }
}
