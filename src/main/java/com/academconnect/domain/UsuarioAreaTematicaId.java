package com.academconnect.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class UsuarioAreaTematicaId implements Serializable {

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "area_id")
    private Long areaId;

    public UsuarioAreaTematicaId() {}

    public UsuarioAreaTematicaId(Long usuarioId, Long areaId) {
        this.usuarioId = usuarioId;
        this.areaId = areaId;
    }

    public Long getUsuarioId() { return usuarioId; }
    public Long getAreaId() { return areaId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioAreaTematicaId other)) return false;
        return Objects.equals(usuarioId, other.usuarioId) && Objects.equals(areaId, other.areaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, areaId);
    }
}
