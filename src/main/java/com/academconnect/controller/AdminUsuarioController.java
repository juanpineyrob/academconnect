package com.academconnect.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/** Endpoints administrativos sobre Usuario (configuración de capacidad, etc.). */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioRepository usuarioRepository;

    /** G08 — admin ajusta el tope de asignaciones de un evaluador. */
    @PatchMapping("/{id}/tope")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void ajustarTope(@PathVariable Long id, @RequestParam int tope) {
        if (tope < 0) {
            throw new IllegalArgumentException("tope debe ser >= 0");
        }
        var u = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        u.setTopeAsignaciones(tope);
        usuarioRepository.save(u);
    }
}
