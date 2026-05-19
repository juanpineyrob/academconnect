package com.academconnect.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.dto.AsignacionResponse;
import com.academconnect.dto.EvaluacionResponse;
import com.academconnect.dto.PerfilResponse;
import com.academconnect.dto.PerfilUpdateRequest;
import com.academconnect.dto.UsuarioAreaTematicaResponse;
import com.academconnect.dto.UsuarioAreasRequest;
import com.academconnect.service.AsignacionService;
import com.academconnect.service.EvaluacionService;
import com.academconnect.service.PerfilService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MeController {

    private final AsignacionService asignacionService;
    private final EvaluacionService evaluacionService;
    private final PerfilService perfilService;

    @GetMapping("/me/perfil")
    @PreAuthorize("isAuthenticated()")
    public PerfilResponse obtenerPerfil(@AuthenticationPrincipal Jwt jwt) {
        return perfilService.obtenerPerfil(jwt.getSubject());
    }

    @PutMapping("/me/perfil")
    @PreAuthorize("isAuthenticated()")
    public PerfilResponse actualizarPerfil(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PerfilUpdateRequest request) {
        return perfilService.actualizarPerfil(jwt.getSubject(), request);
    }

    @GetMapping("/me/areas")
    @PreAuthorize("isAuthenticated()")
    public PerfilResponse obtenerPerfilConAreas(@AuthenticationPrincipal Jwt jwt) {
        return perfilService.obtenerPerfil(jwt.getSubject());
    }

    @PutMapping("/me/areas")
    @PreAuthorize("isAuthenticated()")
    public List<UsuarioAreaTematicaResponse> actualizarAreas(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UsuarioAreasRequest request) {
        return perfilService.actualizarAreas(jwt.getSubject(), request);
    }

    @GetMapping("/evaluador/me/asignaciones")
    @PreAuthorize("hasAnyRole('PROFESOR','EXTERNO')")
    public List<AsignacionResponse> misAsignaciones(@AuthenticationPrincipal Jwt jwt) {
        return asignacionService.listarMisAsignaciones(jwt.getSubject());
    }

    @GetMapping("/estudiante/me/trabajos/{trabajoId}/nota")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public List<EvaluacionResponse> notaTrabajo(
            @PathVariable Long trabajoId,
            @AuthenticationPrincipal Jwt jwt) {
        return evaluacionService.listarNotasTrabajo(trabajoId, jwt.getSubject());
    }
}
