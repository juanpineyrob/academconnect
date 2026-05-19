package com.academconnect.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.dto.AsignacionResponse;
import com.academconnect.dto.EvaluacionResponse;
import com.academconnect.service.AsignacionService;
import com.academconnect.service.EvaluacionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MeController {

    private final AsignacionService asignacionService;
    private final EvaluacionService evaluacionService;

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
