package com.academconnect.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.dto.ActividadResponse;
import com.academconnect.service.ActividadService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ActividadController {

    private final ActividadService service;

    @GetMapping("/me/actividad")
    @PreAuthorize("isAuthenticated()")
    public List<ActividadResponse> feed(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "20") int limit) {
        return service.feed(jwt.getSubject(), limit);
    }

    @GetMapping("/admin/actividad")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Page<ActividadResponse> feedAdmin(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.feedAdmin(pageable);
    }
}
