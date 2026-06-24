package com.academconnect.dto;

import java.util.List;

public record ImportPreviewResponse(Long loteId, int total, int nuevos, int existentes, int errores,
        List<ImportItemResponse> items) {
}
