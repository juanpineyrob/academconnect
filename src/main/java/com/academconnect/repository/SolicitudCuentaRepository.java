package com.academconnect.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academconnect.domain.EstadoSolicitudCuenta;
import com.academconnect.domain.SolicitudCuenta;

public interface SolicitudCuentaRepository extends JpaRepository<SolicitudCuenta, Long> {

    @Query("""
        SELECT s FROM SolicitudCuenta s
        WHERE (:estado IS NULL OR s.estado = :estado)
          AND (:patron IS NULL OR lower(s.nombre) LIKE :patron OR lower(s.email) LIKE :patron OR lower(s.matricula) LIKE :patron)
        ORDER BY s.createdAt DESC
        """)
    Page<SolicitudCuenta> buscar(@Param("estado") EstadoSolicitudCuenta estado,
                                 @Param("patron") String patron, Pageable pageable);

    List<SolicitudCuenta> findByEstadoInAndUpdatedAtBefore(List<EstadoSolicitudCuenta> estados, Instant antes);
}
