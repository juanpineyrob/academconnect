package com.academconnect.domain;

import java.math.BigDecimal;

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
@Table(name = "calificacion_criterio")
@Getter
@Setter
@NoArgsConstructor
public class CalificacionCriterio extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluacion_id", nullable = false)
    private Evaluacion evaluacion;

    /** Matchea con el campo "codigo" del criterio dentro del template_snapshot. */
    @Column(name = "criterio_codigo", nullable = false, length = 100)
    private String criterioCodigo;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal puntaje;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    /** Si true, el comentario solo lo ven evaluadores, orientador y admins; el estudiante queda fuera. */
    @Column(name = "comentario_privado", nullable = false)
    private boolean comentarioPrivado = true;
}
