package com.academconnect.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reconocimiento")
@Getter
@Setter
@NoArgsConstructor
public class Reconocimiento extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false)
    private int anio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "otorgado_por")
    private Usuario otorgadoPor;
}
