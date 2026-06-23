package com.academconnect.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /** Administración: áreas (incluidas inactivas) paginadas y filtradas por texto. */
    @GetMapping("/todas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Page<AreaTematicaResponse> listarTodas(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 10, sort = "nombre") Pageable pageable) {
        return service.buscar(q, pageable);
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
