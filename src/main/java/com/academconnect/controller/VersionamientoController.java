package com.academconnect.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.academconnect.dto.VersionamientoResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.repository.VersionamientoRepository;
import com.academconnect.service.VersionamientoService;
import com.academconnect.service.storage.DocumentoStorage;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trabajos/{trabajoId}/versiones")
@RequiredArgsConstructor
public class VersionamientoController {

    private final VersionamientoService service;
    private final VersionamientoRepository versionamientoRepository;
    private final DocumentoStorage documentoStorage;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<VersionamientoResponse> listar(
            @PathVariable Long trabajoId,
            @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        return service.listarPorTrabajo(trabajoId, includeDeleted);
    }

    @GetMapping("/{versionId}")
    @PreAuthorize("isAuthenticated()")
    public VersionamientoResponse buscarPorId(
            @PathVariable Long trabajoId,
            @PathVariable Long versionId) {
        return service.buscarPorId(versionId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ESTUDIANTE','PROFESOR')")
    public VersionamientoResponse crear(
            @PathVariable Long trabajoId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String comentario) {
        return service.crear(trabajoId, file, comentario);
    }

    @PutMapping(path = "/{versionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public VersionamientoResponse reemplazar(
            @PathVariable Long trabajoId,
            @PathVariable Long versionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String comentario,
            Authentication authn) {
        return service.reemplazar(trabajoId, versionId, file, comentario, currentUserId(authn));
    }

    @DeleteMapping("/{versionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public void eliminar(
            @PathVariable Long trabajoId,
            @PathVariable Long versionId,
            Authentication authn) {
        service.eliminar(trabajoId, versionId, currentUserId(authn));
    }

    @GetMapping("/{versionId}/documento")
    @PreAuthorize("isAuthenticated()")
    // Con OSIV desactivado y `documento` LAZY, hay que mantener la sesión abierta
    // mientras se leen sus campos (storageKey, mimeType, etc.); si no, LazyInitializationException.
    @Transactional(readOnly = true)
    public ResponseEntity<InputStreamResource> descargar(
            @PathVariable Long trabajoId,
            @PathVariable Long versionId,
            @RequestParam(required = false, defaultValue = "false") boolean inline) throws IOException {
        var version = versionamientoRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Versionamiento", versionId));
        var doc = version.getDocumento();

        if (!documentoStorage.exists(doc.getStorageKey())) {
            throw new BusinessException("Archivo no encontrado en el almacenamiento");
        }

        // Por defecto `attachment` (descarga; p. ej. el link "Descargar" de mis-trabajos).
        // `?inline=true` permite incrustarlo en un visor embebido (pantalla de evaluar).
        var disposition = (inline
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(doc.getNombreOriginal())
                .build();

        var stream = documentoStorage.retrieve(doc.getStorageKey());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getMimeType()))
                .contentLength(doc.getSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(stream));
    }

    private Long currentUserId(Authentication authn) {
        var email = authn.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email))
                .getId();
    }
}
