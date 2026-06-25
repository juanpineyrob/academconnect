package com.academconnect.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solicitud_coorientacion")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudCoorientacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trabajo_id", nullable = false)
    private Trabajo trabajo;

    /** Invitado a coorientar: puede ser un Profesor o un Externo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitado_id", nullable = false)
    private Usuario invitado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 40)
    private EstadoInvitacion estado;

    @Column(name = "motivo", columnDefinition = "text")
    private String motivo;

    @Column(name = "respuesta", columnDefinition = "text")
    private String respuesta;

    @Column(name = "resuelta_en")
    private Instant resueltaEn;
}
