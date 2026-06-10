package com.academconnect.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.dto.PerfilPublicoResponse;
import com.academconnect.service.PerfilService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final PerfilService perfilService;

    /** Lectura pública: perfil sin datos sensibles. */
    @GetMapping("/{id}/perfil")
    public PerfilPublicoResponse perfilPublico(@PathVariable Long id) {
        return perfilService.buscarPerfilPublico(id);
    }
}
