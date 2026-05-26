package com.academconnect.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.academconnect.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Profesores y externos activos: pool de evaluadores potenciales. */
    @Query("SELECT COUNT(u) FROM Usuario u " +
           "WHERE u.activo = true AND TYPE(u) IN (com.academconnect.domain.Profesor, com.academconnect.domain.Externo)")
    long contarEvaluadoresActivos();
}
