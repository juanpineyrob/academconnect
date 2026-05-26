package com.academconnect.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.domain.EstadoAsignacion;
import com.academconnect.dto.AsignacionResponse;
import com.academconnect.dto.CargaEvaluadorResponse;
import com.academconnect.dto.EvaluacionResponse;
import com.academconnect.dto.PerfilResponse;
import com.academconnect.dto.PerfilUpdateRequest;
import com.academconnect.dto.StatsEvaluadorResponse;
import com.academconnect.dto.TrabajoResponse;
import com.academconnect.dto.UsuarioAreaTematicaResponse;
import com.academconnect.dto.UsuarioAreasRequest;
import com.academconnect.service.AsignacionService;
import com.academconnect.service.EvaluacionService;
import com.academconnect.service.MetricasService;
import com.academconnect.service.PerfilService;
import com.academconnect.service.TrabajoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MeController {

    private final AsignacionService asignacionService;
    private final EvaluacionService evaluacionService;
    private final PerfilService perfilService;
    private final TrabajoService trabajoService;
    private final MetricasService metricasService;

    @GetMapping("/me/perfil")
    @PreAuthorize("isAuthenticated()")
    public PerfilResponse obtenerPerfil(@AuthenticationPrincipal Jwt jwt) {
        return perfilService.obtenerPerfil(jwt.getSubject());
    }

    @PutMapping("/me/perfil")
    @PreAuthorize("isAuthenticated()")
    public PerfilResponse actualizarPerfil(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PerfilUpdateRequest request) {
        return perfilService.actualizarPerfil(jwt.getSubject(), request);
    }

    @GetMapping("/me/areas")
    @PreAuthorize("isAuthenticated()")
    public PerfilResponse obtenerPerfilConAreas(@AuthenticationPrincipal Jwt jwt) {
        return perfilService.obtenerPerfil(jwt.getSubject());
    }

    @PutMapping("/me/areas")
    @PreAuthorize("isAuthenticated()")
    public List<UsuarioAreaTematicaResponse> actualizarAreas(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UsuarioAreasRequest request) {
        return perfilService.actualizarAreas(jwt.getSubject(), request);
    }

    /**
     * G09 — historial del evaluador. {@code estado} es opcional:
     * default {@code ACTIVA}; {@code ALL} (sin valor de enum) devuelve todo.
     */
    @GetMapping("/evaluador/me/asignaciones")
    @PreAuthorize("hasAnyRole('PROFESOR','EXTERNO')")
    public List<AsignacionResponse> misAsignaciones(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) EstadoAsignacion estado,
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        if (all) {
            return asignacionService.listarMisAsignacionesTodas(jwt.getSubject());
        }
        return asignacionService.listarMisAsignaciones(jwt.getSubject(),
                estado != null ? estado : EstadoAsignacion.ACTIVA);
    }

    /** G03 — trabajos del estudiante autenticado. */
    @GetMapping("/estudiante/me/trabajos")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public List<TrabajoResponse> misTrabajos(@AuthenticationPrincipal Jwt jwt) {
        return trabajoService.listarMisTrabajos(jwt.getSubject());
    }

    /** G08 — carga actual vs tope + disponibilidad semanal próxima. */
    @GetMapping("/evaluador/me/carga")
    @PreAuthorize("hasAnyRole('PROFESOR','EXTERNO')")
    public CargaEvaluadorResponse miCarga(@AuthenticationPrincipal Jwt jwt) {
        return metricasService.cargaEvaluador(jwt.getSubject());
    }

    /** G22 — estadísticas del evaluador autenticado. */
    @GetMapping("/evaluador/me/stats")
    @PreAuthorize("hasAnyRole('PROFESOR','EXTERNO')")
    public StatsEvaluadorResponse misStats(@AuthenticationPrincipal Jwt jwt) {
        return metricasService.statsEvaluador(jwt.getSubject());
    }

    @GetMapping("/estudiante/me/trabajos/{trabajoId}/nota")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public List<EvaluacionResponse> notaTrabajo(
            @PathVariable Long trabajoId,
            @AuthenticationPrincipal Jwt jwt) {
        return evaluacionService.listarNotasTrabajo(trabajoId, jwt.getSubject());
    }
}
