package com.academconnect.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.domain.EstadoSesionEvaluacion;
import com.academconnect.dto.SesionEvaluacionRequest;
import com.academconnect.dto.SesionEvaluacionResponse;
import com.academconnect.service.SesionEvaluacionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sesiones")
@RequiredArgsConstructor
public class SesionEvaluacionController {

    private final SesionEvaluacionService service;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public SesionEvaluacionResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<SesionEvaluacionResponse> listarPorTrabajo(@RequestParam Long trabajoId) {
        return service.listarPorTrabajo(trabajoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PROFESOR','ADMINISTRADOR')")
    public SesionEvaluacionResponse crear(@Valid @RequestBody SesionEvaluacionRequest request) {
        return service.crear(request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('PROFESOR','ADMINISTRADOR')")
    public SesionEvaluacionResponse cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoSesionEvaluacion nuevo) {
        return service.cambiarEstado(id, nuevo);
    }

    /** G20 — agrupa una asignación dentro de la sesión sincrónica (defensa en vivo, etc.). */
    @PostMapping("/{id}/asignaciones/{asignacionId}")
    @PreAuthorize("hasAnyRole('PROFESOR','ADMINISTRADOR')")
    public SesionEvaluacionResponse vincularAsignacion(
            @PathVariable Long id,
            @PathVariable Long asignacionId) {
        return service.vincularAsignacion(id, asignacionId);
    }
}
