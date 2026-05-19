package com.academconnect.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.dto.AreaTematicaResponse;
import com.academconnect.service.AreaTematicaService;

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

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public AreaTematicaResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }
}
