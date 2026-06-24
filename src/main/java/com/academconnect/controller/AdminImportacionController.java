package com.academconnect.controller;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.academconnect.dto.ImportConfirmRequest;
import com.academconnect.dto.ImportPreviewResponse;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.service.ImportacionUsuariosService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Importación masiva de estudiantes: preview (dry-run multipart) y confirmar (commit). */
@RestController
@RequestMapping("/admin/importaciones")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminImportacionController {

    private final ImportacionUsuariosService service;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/preview")
    public ImportPreviewResponse preview(@RequestParam("file") MultipartFile file, Authentication authn) {
        try {
            return service.preview(file.getOriginalFilename(), file.getBytes(), callerId(authn));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @PostMapping("/{id}/confirmar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmar(@PathVariable Long id, @Valid @RequestBody ImportConfirmRequest req,
                          Authentication authn) {
        service.confirmar(id, req, callerId(authn));
    }

    private Long callerId(Authentication authn) {
        return usuarioRepository.findByEmail(authn.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", authn.getName()))
                .getId();
    }
}
