package com.academconnect.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instancia_evaluacion_config")
@Getter
@Setter
@NoArgsConstructor
public class InstanciaEvaluacionConfig extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoTrabajo tipo;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(name = "evaluadores_requeridos", nullable = false)
    private int evaluadoresRequeridos;
}
