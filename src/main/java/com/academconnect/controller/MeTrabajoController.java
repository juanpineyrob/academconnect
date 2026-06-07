package com.academconnect.controller;

import com.academconnect.dto.TrabajoEstudianteRequest;
import com.academconnect.dto.TrabajoResponse;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.service.TrabajoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/trabajos")
@RequiredArgsConstructor
public class MeTrabajoController {

    private final TrabajoService service;
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public TrabajoResponse crear(@Valid @RequestBody TrabajoEstudianteRequest request, Authentication authn) {
        return service.crearPorEstudiante(request, currentUserId(authn));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public TrabajoResponse actualizar(@PathVariable Long id,
                                      @Valid @RequestBody TrabajoEstudianteRequest request,
                                      Authentication authn) {
        return service.actualizarBorradorPorEstudiante(id, request, currentUserId(authn));
    }

    @GetMapping
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public List<TrabajoResponse> listar(Authentication authn) {
        return service.listarMisBorradores(currentUserId(authn));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public TrabajoResponse buscarPorId(@PathVariable Long id, Authentication authn) {
        var trabajo = service.buscarPorId(id);
        if (trabajo.estudianteId() == null || !trabajo.estudianteId().equals(currentUserId(authn))) {
            throw new ResourceNotFoundException("Trabajo", id);
        }
        return trabajo;
    }

    private Long currentUserId(Authentication authn) {
        var email = authn.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email))
                .getId();
    }
}
