package com.academconnect.dto;

import java.time.Instant;

public record VersionamientoResponse(
        Long id,
        Long trabajoId,
        int numeroVersion,
        String comentario,
        DocumentoResponse documento,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy,
        Instant deletedAt,
        String deletedBy) {
}
