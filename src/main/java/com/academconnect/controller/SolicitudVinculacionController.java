package com.academconnect.controller;

import com.academconnect.dto.RespuestaSolicitudRequest;
import com.academconnect.dto.SolicitudVinculacionRequest;
import com.academconnect.dto.SolicitudVinculacionResponse;
import com.academconnect.service.SolicitudVinculacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudVinculacionController {

    private final SolicitudVinculacionService service;

    @GetMapping("/{id}")
    public SolicitudVinculacionResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudVinculacionResponse crear(@Valid @RequestBody SolicitudVinculacionRequest request) {
        return service.crear(request);
    }

    @PostMapping("/{id}/aceptar")
    public SolicitudVinculacionResponse aceptar(
            @PathVariable Long id,
            @RequestBody(required = false) RespuestaSolicitudRequest request) {
        return service.aceptar(id, request);
    }

    @PostMapping("/{id}/rechazar")
    public SolicitudVinculacionResponse rechazar(
            @PathVariable Long id,
            @RequestBody(required = false) RespuestaSolicitudRequest request) {
        return service.rechazar(id, request);
    }
}
