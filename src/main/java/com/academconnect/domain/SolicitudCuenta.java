package com.academconnect.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "solicitud_cuenta")
@Getter
@Setter
public class SolicitudCuenta extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String matricula;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitudCuenta estado = EstadoSolicitudCuenta.PENDIENTE;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(name = "decidido_por_id")
    private Long decididoPorId;

    @Column(name = "decidido_en")
    private Instant decididoEn;
}
