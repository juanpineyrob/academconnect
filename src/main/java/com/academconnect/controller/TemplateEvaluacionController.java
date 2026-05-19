package com.academconnect.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.dto.TemplateEvaluacionRequest;
import com.academconnect.dto.TemplateEvaluacionResponse;
import com.academconnect.service.TemplateEvaluacionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateEvaluacionController {

    private final TemplateEvaluacionService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<TemplateEvaluacionResponse> listar() {
        return service.listarActivos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public TemplateEvaluacionResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public TemplateEvaluacionResponse crear(@Valid @RequestBody TemplateEvaluacionRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public TemplateEvaluacionResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody TemplateEvaluacionRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void desactivar(@PathVariable Long id) {
        service.desactivar(id);
    }
}
