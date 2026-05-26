package com.academconnect.domain;

import java.util.ArrayList;
import java.util.List;

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
@Table(name = "actividad")
@Getter
@Setter
@NoArgsConstructor
public class Actividad extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private TipoActividad tipo;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "recurso_tipo", nullable = false, length = 40)
    private String recursoTipo;

    @Column(name = "recurso_id", nullable = false)
    private Long recursoId;

    /** Payload renderizable en frontend: {actorNombre, recursoTitulo, ...}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisibilidadActividad visibilidad;

    /** Ids de usuarios vinculados al recurso (para visibility=PARTICIPANTES). */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "participantes_ids", nullable = false, columnDefinition = "bigint[]")
    private List<Long> participantesIds = new ArrayList<>();
}
