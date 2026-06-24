package com.academconnect.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Fila clasificada del lote de importación (resultado del preview). */
@Entity
@Table(name = "lote_importacion_item")
@Getter
@Setter
public class LoteImportacionItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    private LoteImportacion lote;

    @Column(nullable = false)
    private int linea;

    @Column(length = 30)
    private String matricula;

    @Column(length = 255)
    private String email;

    @Column(length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ResultadoFila resultado;

    @Column(length = 500)
    private String detalle;
}
