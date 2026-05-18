package com.academconnect.dto;

import com.academconnect.domain.NivelExperticia;

public record UsuarioAreaTematicaResponse(
        Long areaId,
        String areaNombre,
        NivelExperticia nivelExperticia) {
}
