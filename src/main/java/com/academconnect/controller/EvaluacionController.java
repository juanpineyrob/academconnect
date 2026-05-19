package com.academconnect.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.dto.EvaluacionRequest;
import com.academconnect.dto.EvaluacionResponse;
import com.academconnect.service.EvaluacionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EvaluacionController {

    private final EvaluacionService service;

    @GetMapping("/evaluaciones/{id}")
    @PreAuthorize("isAuthenticated()")
    public EvaluacionResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/asignaciones/{asignacionId}/evaluacion")
    @PreAuthorize("isAuthenticated()")
    public EvaluacionResponse buscarPorAsignacion(@PathVariable Long asignacionId) {
        return service.buscarPorAsignacion(asignacionId);
    }

    @PostMapping("/evaluaciones")
    @PreAuthorize("hasAnyRole('PROFESOR','EXTERNO')")
    public ResponseEntity<EvaluacionResponse> completar(@Valid @RequestBody EvaluacionRequest request) {
        return ResponseEntity.status(201).body(service.completar(request));
    }
}
