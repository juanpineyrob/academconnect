package com.academconnect.domain;

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
@Table(name = "sesion_evaluacion")
@Getter
@Setter
@NoArgsConstructor
public class SesionEvaluacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trabajo_id", nullable = false)
    private Trabajo trabajo;

    @Column(name = "fecha_programada", nullable = false)
    private Instant fechaProgramada;

    @Column(name = "duracion_minutos", nullable = false)
    private int duracionMinutos;

    @Column(length = 255)
    private String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModalidadSesion modalidad;

    @Column(name = "url_meet", length = 500)
    private String urlMeet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSesionEvaluacion estado = EstadoSesionEvaluacion.PROGRAMADA;
}
