package com.academconnect.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.domain.TipoTrabajo;
import com.academconnect.dto.TipoTrabajoConfigRequest;
import com.academconnect.dto.TipoTrabajoConfigResponse;
import com.academconnect.service.TipoTrabajoConfigService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/tipos-trabajo-config")
public class TipoTrabajoConfigController {

    private final TipoTrabajoConfigService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<TipoTrabajoConfigResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{tipo}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public TipoTrabajoConfigResponse buscarPorTipo(@PathVariable TipoTrabajo tipo) {
        return service.buscarPorTipo(tipo);
    }

    /** Upsert: si la config no existe se crea, si existe se actualiza. */
    @PutMapping("/{tipo}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public TipoTrabajoConfigResponse actualizar(
            @PathVariable TipoTrabajo tipo,
            @Valid @RequestBody TipoTrabajoConfigRequest request) {
        return service.actualizar(tipo, request);
    }
}
