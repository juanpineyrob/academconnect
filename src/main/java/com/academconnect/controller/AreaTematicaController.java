package com.academconnect.controller;

import com.academconnect.dto.AreaTematicaResponse;
import com.academconnect.service.AreaTematicaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/areas-tematicas")
@RequiredArgsConstructor
public class AreaTematicaController {

    private final AreaTematicaService service;

    @GetMapping
    public List<AreaTematicaResponse> listar() {
        return service.listarActivas();
    }

    @GetMapping("/{id}")
    public AreaTematicaResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }
}
