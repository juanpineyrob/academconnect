package com.academconnect.dto;

import java.util.List;

import com.academconnect.domain.NivelExperticia;

import jakarta.validation.constraints.NotNull;

public record UsuarioAreasRequest(@NotNull List<AreaConNivelRequest> areas) {

    public record AreaConNivelRequest(@NotNull Long areaId, NivelExperticia nivelExperticia) {}
}
