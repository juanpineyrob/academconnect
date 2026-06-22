package com.academconnect.dto;

import com.academconnect.domain.ThesaurusOrigen;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AreaTematicaRequest(
        @NotBlank @Size(max = 200) String nombre,
        @Size(max = 50) String codigoExterno,
        @NotNull ThesaurusOrigen thesaurusOrigen,
        Long parentId) {
}
