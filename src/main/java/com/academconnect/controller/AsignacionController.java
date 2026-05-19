package com.academconnect.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.dto.AsignacionRequest;
import com.academconnect.dto.AsignacionResponse;
import com.academconnect.service.AsignacionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AsignacionController {

    private final AsignacionService service;

    @GetMapping("/asignaciones/{id}")
    @PreAuthorize("isAuthenticated()")
    public AsignacionResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/trabajos/{trabajoId}/asignaciones")
    @PreAuthorize("isAuthenticated()")
    public List<AsignacionResponse> listarPorTrabajo(@PathVariable Long trabajoId) {
        return service.listarPorTrabajo(trabajoId);
    }

    @PostMapping("/asignaciones")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignacionResponse> crear(@Valid @RequestBody AsignacionRequest request) {
        return ResponseEntity.status(201).body(service.crear(request));
    }

    @PostMapping("/asignaciones/{id}/cancelar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AsignacionResponse cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }
}
