package com.academconnect.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.dto.SolicitudVinculacionResponse;
import com.academconnect.dto.TrabajoRequest;
import com.academconnect.dto.TrabajoResponse;
import com.academconnect.service.SolicitudVinculacionService;
import com.academconnect.service.TrabajoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trabajos")
@RequiredArgsConstructor
public class TrabajoController {

    private final TrabajoService service;
    private final SolicitudVinculacionService solicitudService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<TrabajoResponse> listar(
            @RequestParam(required = false) EstadoTrabajo estado,
            @RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) return service.buscarPorTexto(q);
        if (estado != null) return service.listarPorEstado(estado);
        return service.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public TrabajoResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROFESOR')")
    public TrabajoResponse crear(@Valid @RequestBody TrabajoRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public TrabajoResponse actualizar(@PathVariable Long id, @Valid @RequestBody TrabajoRequest request) {
        return service.actualizar(id, request);
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public TrabajoResponse aprobar(@PathVariable Long id) {
        return service.aprobar(id);
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public TrabajoResponse rechazar(@PathVariable Long id) {
        return service.rechazar(id);
    }

    @GetMapping("/{id}/solicitudes")
    @PreAuthorize("isAuthenticated()")
    public List<SolicitudVinculacionResponse> listarSolicitudes(@PathVariable Long id) {
        return solicitudService.listarPorTrabajo(id);
    }
}
