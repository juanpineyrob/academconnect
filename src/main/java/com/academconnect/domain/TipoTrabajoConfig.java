package com.academconnect.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Config por tipo de trabajo: modo de evaluación y default de evaluadores. */
@Entity
@Table(name = "tipo_trabajo_config")
@Getter
@Setter
@NoArgsConstructor
public class TipoTrabajoConfig {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoTrabajo tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_evaluacion", nullable = false, length = 20)
    private ModoEvaluacion modoEvaluacion;

    @Column(name = "evaluadores_default", nullable = false)
    private int evaluadoresDefault = 3;
}
