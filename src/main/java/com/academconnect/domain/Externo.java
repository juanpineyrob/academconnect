package com.academconnect.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "externo")
@DiscriminatorValue("EXTERNO")
@Getter
@Setter
@NoArgsConstructor
public class Externo extends Usuario {

    @Column(nullable = false, length = 200)
    private String institucion;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Override
    public Rol getRol() {
        return Rol.EXTERNO;
    }
}
