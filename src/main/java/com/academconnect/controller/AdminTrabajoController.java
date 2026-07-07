package com.academconnect.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.dto.TrabajoAdminImportRequest;
import com.academconnect.dto.TrabajoResponse;
import com.academconnect.service.TrabajoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints administrativos sobre trabajos académicos. Sólo accesibles para usuarios con rol
 * ADMINISTRADOR. Permite importar trabajos legacy (finalizados fuera del sistema) directamente
 * en su estado final sin pasar por las transiciones de evaluación.
 */
@RestController
@RequestMapping("/admin/trabajos")
@RequiredArgsConstructor
public class AdminTrabajoController {

    private final TrabajoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public TrabajoResponse importar(@Valid @RequestBody TrabajoAdminImportRequest request) {
        return service.importarLegacy(request);
    }
}
