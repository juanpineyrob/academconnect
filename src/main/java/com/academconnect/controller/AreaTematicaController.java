package com.academconnect.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.dto.AreaTematicaRequest;
import com.academconnect.dto.AreaTematicaResponse;
import com.academconnect.service.AreaTematicaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/areas-tematicas")
@RequiredArgsConstructor
public class AreaTematicaController {

    private final AreaTematicaService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<AreaTematicaResponse> listar() {
        return service.listarActivas();
    }

    /** Administración: todas las áreas, incluidas las inactivas. */
    @GetMapping("/todas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<AreaTematicaResponse> listarTodas() {
        return service.listarTodas();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public AreaTematicaResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AreaTematicaResponse crear(@Valid @RequestBody AreaTematicaRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AreaTematicaResponse actualizar(@PathVariable Long id,
                                           @Valid @RequestBody AreaTematicaRequest request) {
        return service.actualizar(id, request);
    }

    @PostMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AreaTematicaResponse activar(@PathVariable Long id) {
        return service.setActivo(id, true);
    }

    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AreaTematicaResponse desactivar(@PathVariable Long id) {
        return service.setActivo(id, false);
    }
}
