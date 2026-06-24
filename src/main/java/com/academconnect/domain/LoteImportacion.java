package com.academconnect.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Lote de importación masiva de estudiantes. PREVIEW (dry-run) → CONFIRMADO (commit). */
@Entity
@Table(name = "lote_importacion")
@Getter
@Setter
public class LoteImportacion extends BaseEntity {

    @Column(name = "archivo_hash", nullable = false, length = 64)
    private String archivoHash;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoLote estado = EstadoLote.PREVIEW;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false)
    private int nuevos;

    @Column(nullable = false)
    private int existentes;

    @Column(nullable = false)
    private int errores;

    @Column(name = "creado_por_id")
    private Long creadoPorId;

    @OneToMany(mappedBy = "lote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoteImportacionItem> items = new ArrayList<>();
}
