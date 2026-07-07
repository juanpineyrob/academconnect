package com.academconnect.domain;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "asignacion")
@Getter
@Setter
@NoArgsConstructor
public class Asignacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trabajo_id", nullable = false)
    private Trabajo trabajo;

    /** Versión específica congelada a evaluar — garantiza inmutabilidad del contenido evaluado. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "versionamiento_id", nullable = false)
    private Versionamiento versionamiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluador_id", nullable = false)
    private Usuario evaluador;

    /**
     * Copia inmutable de la rúbrica al momento en que el evaluador la elige; el template
     * puede mutar después. NULL hasta que el evaluador selecciona una rúbrica al entrar.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_snapshot", columnDefinition = "jsonb")
    private String templateSnapshot;

    @Column(name = "asignada_en", nullable = false)
    private Instant asignadaEn;

    @Column(name = "vencimiento_en")
    private Instant vencimientoEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsignacion estado;

    /** Sesión de evaluación (sincrónica) a la que pertenece esta asignación. Null para asincrónicas. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_id")
    private SesionEvaluacion sesion;

    /** Instancia de evaluación a la que pertenece esta asignación. Null para asignaciones legacy (ronda única). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instancia_evaluacion_id")
    private InstanciaEvaluacion instanciaEvaluacion;
}
