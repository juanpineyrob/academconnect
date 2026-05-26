package com.academconnect.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.dto.StatsPublicasResponse;
import com.academconnect.service.MetricasService;

import lombok.RequiredArgsConstructor;

/** G02 — endpoints públicos (sin autenticación). Whitelisted en SecurityConfig. */
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicStatsController {

    private final MetricasService metricasService;

    @GetMapping("/stats")
    public StatsPublicasResponse stats() {
        return metricasService.statsPublicas();
    }
}
