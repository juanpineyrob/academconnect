package com.academconnect.dto;

import com.academconnect.domain.ThesaurusOrigen;

public record AreaTematicaResponse(
        Long id,
        String codigoExterno,
        String nombre,
        Long parentId,
        ThesaurusOrigen thesaurusOrigen,
        boolean activo) {
}
