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

import com.academconnect.dto.ProfesorRequest;
import com.academconnect.dto.ProfesorResponse;
import com.academconnect.service.ProfesorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/profesores")
@RequiredArgsConstructor
public class ProfesorController {

    private final ProfesorService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ProfesorResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ProfesorResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ProfesorResponse crear(@Valid @RequestBody ProfesorRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ProfesorResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProfesorRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void desactivar(@PathVariable Long id) {
        service.desactivar(id);
    }
}
