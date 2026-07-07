package com.academconnect.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/export.csv")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> exportarCsv() {
        byte[] body = service.exportarCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"metricas-academconnect.csv\"")
                .body(body);
    }
}
