package com.academconnect.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "disponibilidad_evaluador")
@Getter
@Setter
@NoArgsConstructor
public class DisponibilidadEvaluador extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluador_id", nullable = false)
    private Usuario evaluador;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "horas_disponibles", nullable = false, precision = 4, scale = 2)
    private BigDecimal horasDisponibles;
}
