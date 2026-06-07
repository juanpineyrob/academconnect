package com.academconnect.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.dto.InvitacionOrientacionResponse;
import com.academconnect.dto.SolicitudVinculacionResponse;
import com.academconnect.dto.TrabajoRequest;
import com.academconnect.dto.TrabajoResponse;
import com.academconnect.service.InvitacionOrientacionService;
import com.academconnect.service.SolicitudVinculacionService;
import com.academconnect.service.TrabajoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trabajos")
@RequiredArgsConstructor
public class TrabajoController {

    private final TrabajoService service;
    private final SolicitudVinculacionService solicitudService;
    private final InvitacionOrientacionService invitacionService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<TrabajoResponse> listar(
            @RequestParam(required = false) EstadoTrabajo estado,
            @RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) return service.buscarPorTexto(q);
        if (estado != null) return service.listarPorEstado(estado);
        return service.listar();
    }

    /**
     * G12+G13 — búsqueda multi-parámetro combinable. Endpoint público (sin auth) que solo
     * devuelve trabajos en estado APROBADO; cuando el caller está autenticado se relajan los
     * filtros (admite cualquier {@code estado}, {@code orientadorId}).
     */
    @GetMapping("/buscar")
    public Page<TrabajoResponse> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<Long> areaId,
            @RequestParam(required = false) List<Integer> anio,
            @RequestParam(required = false) TipoTrabajo tipo,
            @RequestParam(required = false) EstadoTrabajo estado,
            @RequestParam(required = false) Long orientadorId,
            @RequestParam(required = false) Long estudianteId,
            Authentication authentication,
            Pageable pageable) {
        boolean soloPublicos = authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal());
        return service.buscar(q, areaId, anio, tipo, estado, orientadorId, estudianteId, soloPublicos, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public TrabajoResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROFESOR')")
    public TrabajoResponse crear(@Valid @RequestBody TrabajoRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public TrabajoResponse actualizar(@PathVariable Long id, @Valid @RequestBody TrabajoRequest request) {
        return service.actualizar(id, request);
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public TrabajoResponse aprobar(@PathVariable Long id) {
        return service.aprobar(id);
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public TrabajoResponse rechazar(@PathVariable Long id) {
        return service.rechazar(id);
    }

    @GetMapping("/{id}/solicitudes")
    @PreAuthorize("isAuthenticated()")
    public List<SolicitudVinculacionResponse> listarSolicitudes(@PathVariable Long id) {
        return solicitudService.listarPorTrabajo(id);
    }

    @GetMapping("/{id}/invitaciones")
    @PreAuthorize("isAuthenticated()")
    public List<InvitacionOrientacionResponse> listarInvitaciones(@PathVariable Long id) {
        return invitacionService.listarPorTrabajo(id);
    }
}
