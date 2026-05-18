package com.academconnect.controller;

import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.dto.SolicitudVinculacionResponse;
import com.academconnect.dto.TrabajoRequest;
import com.academconnect.dto.TrabajoResponse;
import com.academconnect.service.SolicitudVinculacionService;
import com.academconnect.service.TrabajoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trabajos")
@RequiredArgsConstructor
public class TrabajoController {

    private final TrabajoService service;
    private final SolicitudVinculacionService solicitudService;

    @GetMapping
    public List<TrabajoResponse> listar(@RequestParam(required = false) EstadoTrabajo estado) {
        if (estado != null) {
            return service.listarPorEstado(estado);
        }
        return service.listar();
    }

    @GetMapping("/{id}")
    public TrabajoResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrabajoResponse crear(@Valid @RequestBody TrabajoRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public TrabajoResponse actualizar(@PathVariable Long id, @Valid @RequestBody TrabajoRequest request) {
        return service.actualizar(id, request);
    }

    @GetMapping("/{id}/solicitudes")
    public List<SolicitudVinculacionResponse> listarSolicitudes(@PathVariable Long id) {
        return solicitudService.listarPorTrabajo(id);
    }
}
