package com.academconnect.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.dto.MetricasResponse;
import com.academconnect.service.MetricasService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/metricas")
@RequiredArgsConstructor
public class MetricasController {

    private final MetricasService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public MetricasResponse obtenerMetricas() {
        return service.obtenerMetricas();
    }
}
