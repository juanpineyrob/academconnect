package com.academconnect.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.TipoTrabajo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Importación legacy: el administrador da de alta un trabajo ya finalizado (típicamente APROBADO)
 * con autoría conocida y opcionalmente puntaje/evaluación previa. Se omiten transiciones del
 * state machine porque por definición el trabajo nació fuera del sistema.
 */
public record TrabajoAdminImportRequest(
        @NotBlank @Size(max = 300) String titulo,
        String descripcion,
        @NotNull TipoTrabajo tipo,
        @NotNull EstadoTrabajo estado,
        @NotNull Long orientadorId,
        Long estudianteId,
        Set<Long> areaIds,
        @NotNull @Size(min = 3, max = 8) List<@NotBlank @Size(min = 2, max = 50) String> keywords,
        BigDecimal puntajeAgregado,
        Instant evaluadoEn,
        @Size(max = 500) String archivoStorageKey) {
}
