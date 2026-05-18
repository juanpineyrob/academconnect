package com.academconnect.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "recomendacion_evaluador")
@Getter
@Setter
@NoArgsConstructor
public class RecomendacionEvaluador extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trabajo_id", nullable = false)
    private Trabajo trabajo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluador_candidato_id", nullable = false)
    private Usuario evaluadorCandidato;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal score;

    /** JSON con el desglose: { "afinidad": 0.8, "carga_norm": 0.3, "disponibilidad": 1.0 } */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String factores;

    @Column(name = "generada_en", nullable = false)
    private Instant generadaEn;
}
