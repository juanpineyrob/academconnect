package com.academconnect.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario_area_tematica")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioAreaTematica {

    @EmbeddedId
    private UsuarioAreaTematicaId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("areaId")
    @JoinColumn(name = "area_id")
    private AreaTematica area;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_experticia", length = 20)
    private NivelExperticia nivelExperticia;

    public UsuarioAreaTematica(Usuario usuario, AreaTematica area, NivelExperticia nivel) {
        this.usuario = usuario;
        this.area = area;
        this.nivelExperticia = nivel;
        this.id = new UsuarioAreaTematicaId(usuario.getId(), area.getId());
    }
}
