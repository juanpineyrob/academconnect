package com.academconnect.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.dto.ReconocimientoRequest;
import com.academconnect.dto.ReconocimientoResponse;
import com.academconnect.service.ReconocimientoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReconocimientoController {

    private final ReconocimientoService service;

    /** Lectura pública: visible en perfil del usuario. */
    @GetMapping("/usuarios/{usuarioId}/reconocimientos")
    public List<ReconocimientoResponse> listar(@PathVariable Long usuarioId) {
        return service.listarDeUsuario(usuarioId);
    }

    @PostMapping("/admin/usuarios/{usuarioId}/reconocimientos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ReconocimientoResponse otorgar(
            @PathVariable Long usuarioId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReconocimientoRequest request) {
        return service.otorgar(usuarioId, jwt.getSubject(), request);
    }

    @DeleteMapping("/admin/reconocimientos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void revocar(@PathVariable Long id) {
        service.revocar(id);
    }
}
