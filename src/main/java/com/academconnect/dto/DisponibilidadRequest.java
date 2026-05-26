package com.academconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record DisponibilidadRequest(@NotNull @Valid List<Item> items) {

    public record Item(
            @NotNull LocalDate fecha,
            @NotNull @DecimalMin("0.0") @DecimalMax("24.0") BigDecimal horasDisponibles) {}
}
