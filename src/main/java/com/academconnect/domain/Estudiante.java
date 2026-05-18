package com.academconnect.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "estudiante")
@DiscriminatorValue("ESTUDIANTE")
@Getter
@Setter
@NoArgsConstructor
public class Estudiante extends Usuario {

    @Override
    public Rol getRol() {
        return Rol.ESTUDIANTE;
    }
}
