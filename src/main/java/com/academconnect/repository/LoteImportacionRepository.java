package com.academconnect.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.EstadoLote;
import com.academconnect.domain.LoteImportacion;

public interface LoteImportacionRepository extends JpaRepository<LoteImportacion, Long> {

    /** Lotes en un estado dado creados antes de cierto instante (purga de PREVIEW antiguos). */
    List<LoteImportacion> findByEstadoAndCreatedAtBefore(EstadoLote estado, Instant antes);
}
