package com.academconnect.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.domain.Rol;
import com.academconnect.dto.AdminPasswordResetRequest;
import com.academconnect.dto.AdminUsuarioCreateRequest;
import com.academconnect.dto.AdminUsuarioResponse;
import com.academconnect.dto.AdminUsuarioUpdateRequest;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.service.AdminUsuarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Endpoints administrativos sobre Usuario (alta, baja, modificación, capacidad). */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminUsuarioController {

    private final AdminUsuarioService service;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public Page<AdminUsuarioResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Rol rol,
            @PageableDefault(size = 10, sort = "nombre") Pageable pageable) {
        return service.buscar(q, rol, pageable);
    }

    @PostMapping
    public AdminUsuarioResponse crear(@Valid @RequestBody AdminUsuarioCreateRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public AdminUsuarioResponse actualizar(@PathVariable Long id,
                                           @Valid @RequestBody AdminUsuarioUpdateRequest request) {
        return service.actualizar(id, request);
    }

    @PostMapping("/{id}/activar")
    public AdminUsuarioResponse activar(@PathVariable Long id, Authentication authn) {
        return service.setActivo(id, true, callerId(authn));
    }

    @PostMapping("/{id}/desactivar")
    public AdminUsuarioResponse desactivar(@PathVariable Long id, Authentication authn) {
        return service.setActivo(id, false, callerId(authn));
    }

    @PostMapping("/{id}/reset-password")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody AdminPasswordResetRequest request) {
        service.resetPassword(id, request.password());
    }

    /** G08 — admin ajusta el tope de asignaciones de un evaluador. */
    @PatchMapping("/{id}/tope")
    public void ajustarTope(@PathVariable Long id, @RequestParam int tope) {
        if (tope < 0) {
            throw new IllegalArgumentException("tope debe ser >= 0");
        }
        var u = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        u.setTopeAsignaciones(tope);
        usuarioRepository.save(u);
    }

    private Long callerId(Authentication authn) {
        return usuarioRepository.findByEmail(authn.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", authn.getName()))
                .getId();
    }
}
