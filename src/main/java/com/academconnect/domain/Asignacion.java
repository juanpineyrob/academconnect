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

    /** Copia inmutable del template al momento de asignar; el template puede mutar después. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_snapshot", nullable = false, columnDefinition = "jsonb")
    private String templateSnapshot;

    @Column(name = "asignada_en", nullable = false)
    private Instant asignadaEn;

    @Column(name = "vencimiento_en")
    private Instant vencimientoEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsignacion estado;
}
