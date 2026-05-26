package com.academconnect.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.dto.DisponibilidadRequest;
import com.academconnect.dto.DisponibilidadResponse;
import com.academconnect.service.DisponibilidadEvaluadorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/evaluador/me/disponibilidad")
@RequiredArgsConstructor
public class DisponibilidadController {

    private final DisponibilidadEvaluadorService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('PROFESOR','EXTERNO')")
    public List<DisponibilidadResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listarRango(jwt.getSubject(), desde, hasta);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('PROFESOR','EXTERNO')")
    public List<DisponibilidadResponse> guardar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DisponibilidadRequest request) {
        return service.guardar(jwt.getSubject(), request);
    }
}
