package com.academconnect.domain;

import java.math.BigDecimal;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "template_evaluacion")
@Getter
@Setter
@NoArgsConstructor
public class TemplateEvaluacion extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TemplateScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_trabajo_aplicable", length = 40)
    private TipoTrabajo tipoTrabajoAplicable;

    /**
     * JSON con la definición de criterios:
     * [{ "codigo": "metodologia", "nombre": "Metodología", "peso": 0.3, "escalaMin": 0, "escalaMax": 10 }, ...]
     * La validación de schema y suma de pesos se hace en el servicio (Fase 5).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String criterios;

    @Column(nullable = false)
    private boolean activo = true;

    /** Puntaje mínimo (inclusivo) sobre la calificación final agregada para aprobar el trabajo. */
    @Column(name = "umbral_aprobacion", nullable = false, precision = 6, scale = 2)
    private BigDecimal umbralAprobacion;
}
