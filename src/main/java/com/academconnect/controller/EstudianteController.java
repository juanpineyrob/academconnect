package com.academconnect.controller;

import com.academconnect.dto.EstudianteRequest;
import com.academconnect.dto.EstudianteResponse;
import com.academconnect.dto.SolicitudVinculacionResponse;
import com.academconnect.service.EstudianteService;
import com.academconnect.service.SolicitudVinculacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
public class EstudianteController {

    private final EstudianteService service;
    private final SolicitudVinculacionService solicitudService;

    @GetMapping
    public List<EstudianteResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public EstudianteResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstudianteResponse crear(@Valid @RequestBody EstudianteRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public EstudianteResponse actualizar(@PathVariable Long id, @Valid @RequestBody EstudianteRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        service.desactivar(id);
    }

    @GetMapping("/{id}/solicitudes")
    public List<SolicitudVinculacionResponse> listarSolicitudes(@PathVariable Long id) {
        return solicitudService.listarPorEstudiante(id);
    }
}
