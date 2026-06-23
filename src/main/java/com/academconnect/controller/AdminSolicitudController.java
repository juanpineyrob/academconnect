package com.academconnect.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.domain.EstadoSolicitudCuenta;
import com.academconnect.dto.RechazoRequest;
import com.academconnect.dto.SolicitudResponse;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.service.OnboardingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Cola admin de solicitudes de cuenta (self-request): listar, aprobar, rechazar. */
@RestController
@RequestMapping("/admin/solicitudes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminSolicitudController {

    private final OnboardingService onboardingService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public Page<SolicitudResponse> listar(@RequestParam(required = false) EstadoSolicitudCuenta estado,
                                          @RequestParam(required = false) String q,
                                          @PageableDefault(size = 10) Pageable pageable) {
        return onboardingService.buscar(estado, q, pageable);
    }

    @PostMapping("/{id}/aprobar")
    public SolicitudResponse aprobar(@PathVariable Long id, Authentication authn) {
        return onboardingService.aprobar(id, callerId(authn));
    }

    @PostMapping("/{id}/rechazar")
    public SolicitudResponse rechazar(@PathVariable Long id, @Valid @RequestBody RechazoRequest req,
                                      Authentication authn) {
        return onboardingService.rechazar(id, callerId(authn), req.motivo());
    }

    private Long callerId(Authentication authn) {
        return usuarioRepository.findByEmail(authn.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", authn.getName()))
                .getId();
    }
}
