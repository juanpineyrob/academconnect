package com.academconnect.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evaluacion")
@Getter
@Setter
@NoArgsConstructor
public class Evaluacion extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignacion_id", nullable = false, unique = true)
    private Asignacion asignacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEvaluacion estado;

    /** Suma ponderada de las CalificacionCriterio. Se materializa al completar la evaluación. */
    @Column(name = "calificacion_final", precision = 6, scale = 2)
    private BigDecimal calificacionFinal;

    @Column(name = "comentario_general", columnDefinition = "TEXT")
    private String comentarioGeneral;

    @Column(name = "completada_en")
    private Instant completadaEn;

    @OneToMany(mappedBy = "evaluacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CalificacionCriterio> calificaciones = new HashSet<>();
}
