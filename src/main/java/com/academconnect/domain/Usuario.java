package com.academconnect.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING, length = 31)
@Getter
@Setter
public abstract class Usuario extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    private Integer edad;

    @Column(length = 200)
    private String ubicacion;

    @Column(columnDefinition = "TEXT")
    private String biografia;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    /** Tope de asignaciones ACTIVAS simultáneas. Editable por admin. */
    @Column(name = "tope_asignaciones", nullable = false)
    private int topeAsignaciones = 5;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UsuarioAreaTematica> areas = new HashSet<>();

    public abstract Rol getRol();
}
