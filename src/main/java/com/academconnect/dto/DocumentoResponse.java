package com.academconnect.dto;

import java.time.Instant;

public record DocumentoResponse(
        Long id,
        String nombreOriginal,
        String mimeType,
        long sizeBytes,
        String sha256,
        Instant createdAt) {
}
