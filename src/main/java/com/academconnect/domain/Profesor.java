package com.academconnect.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profesor")
@DiscriminatorValue("PROFESOR")
@Getter
@Setter
@NoArgsConstructor
public class Profesor extends Usuario {

    @Column(length = 200)
    private String titulacion;

    @Column(length = 200)
    private String cargo;

    @Override
    public Rol getRol() {
        return Rol.PROFESOR;
    }
}
