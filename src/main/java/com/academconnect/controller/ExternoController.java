package com.academconnect.controller;

import com.academconnect.dto.ExternoRequest;
import com.academconnect.dto.ExternoResponse;
import com.academconnect.service.ExternoService;
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
@RequestMapping("/api/externos")
@RequiredArgsConstructor
public class ExternoController {

    private final ExternoService service;

    @GetMapping
    public List<ExternoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ExternoResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExternoResponse crear(@Valid @RequestBody ExternoRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public ExternoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ExternoRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        service.desactivar(id);
    }
}
