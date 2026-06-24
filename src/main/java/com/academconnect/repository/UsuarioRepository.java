package com.academconnect.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academconnect.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByMatricula(String matricula);

    boolean existsByEmail(String email);

    boolean existsByMatricula(String matricula);

    /** Búsqueda admin paginada (nombre/email/matrícula). {@code patron} ya viene como %…% en minúsculas (o null). */
    @Query("SELECT u FROM Usuario u WHERE :patron IS NULL " +
           "OR LOWER(u.nombre) LIKE :patron " +
           "OR LOWER(u.email) LIKE :patron " +
           "OR LOWER(COALESCE(u.matricula, '')) LIKE :patron")
    Page<Usuario> buscarAdmin(@Param("patron") String patron, Pageable pageable);

    /** Igual que {@link #buscarAdmin} pero filtrando por subtipo (rol). */
    @Query("SELECT u FROM Usuario u WHERE TYPE(u) = :tipo AND (:patron IS NULL " +
           "OR LOWER(u.nombre) LIKE :patron " +
           "OR LOWER(u.email) LIKE :patron " +
           "OR LOWER(COALESCE(u.matricula, '')) LIKE :patron)")
    Page<Usuario> buscarAdminPorTipo(@Param("patron") String patron,
                                     @Param("tipo") Class<? extends Usuario> tipo, Pageable pageable);

    /** Profesores y externos activos: pool de evaluadores potenciales. */
    @Query("SELECT COUNT(u) FROM Usuario u " +
           "WHERE u.activo = true AND TYPE(u) IN (com.academconnect.domain.Profesor, com.academconnect.domain.Externo)")
    long contarEvaluadoresActivos();

    /** Administradores activos: para impedir dejar el sistema sin administradores. */
    @Query("SELECT COUNT(u) FROM Usuario u " +
           "WHERE u.activo = true AND TYPE(u) = com.academconnect.domain.Administrador")
    long contarAdministradoresActivos();
}
