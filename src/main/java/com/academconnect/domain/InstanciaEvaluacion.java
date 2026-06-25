package com.academconnect.domain;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instancia_evaluacion")
@Getter
@Setter
@NoArgsConstructor
public class InstanciaEvaluacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trabajo_id", nullable = false)
    private Trabajo trabajo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instancia_config_id", nullable = false)
    private InstanciaEvaluacionConfig instanciaConfig;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private int intento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoInstanciaEvaluacion estado = EstadoInstanciaEvaluacion.PENDIENTE;

    @Column(name = "puntaje_agregado", precision = 6, scale = 2)
    private BigDecimal puntajeAgregado;

    @Column(name = "cerrada_en")
    private Instant cerradaEn;
}
