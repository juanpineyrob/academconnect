package com.academconnect.dto;

import com.academconnect.domain.EstadoTrabajo;

public record TrabajosPorEstadoDto(EstadoTrabajo estado, Long cantidad) {
}
