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
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "area_tematica")
@Getter
@Setter
@NoArgsConstructor
public class AreaTematica extends BaseEntity {

    @Column(name = "codigo_externo", length = 50)
    private String codigoExterno;

    @Column(nullable = false, length = 200)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private AreaTematica parent;

    @Enumerated(EnumType.STRING)
    @Column(name = "thesaurus_origen", nullable = false, length = 20)
    private ThesaurusOrigen thesaurusOrigen;

    @Column(nullable = false)
    private boolean activo = true;
}
