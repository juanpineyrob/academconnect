package com.academconnect.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academconnect.domain.TemplateEvaluacion;
import com.academconnect.domain.TemplateScope;
import com.academconnect.domain.TipoTrabajo;

public interface TemplateEvaluacionRepository extends JpaRepository<TemplateEvaluacion, Long> {

    List<TemplateEvaluacion> findByActivoTrue();

    List<TemplateEvaluacion> findByScopeAndActivoTrue(TemplateScope scope);

    List<TemplateEvaluacion> findByTipoTrabajoAplicableAndActivoTrue(TipoTrabajo tipo);

    /** Rúbricas propias del usuario (cualquier visibilidad/estado). */
    @Query("SELECT t FROM TemplateEvaluacion t WHERE t.autor.id = :callerId")
    Page<TemplateEvaluacion> buscarMias(@Param("callerId") Long callerId, Pageable pageable);

    /** Rúbricas públicas y activas de otros usuarios. */
    @Query("SELECT t FROM TemplateEvaluacion t WHERE t.visibilidad = com.academconnect.domain.Visibilidad.PUBLICO "
            + "AND t.activo = true AND (t.autor IS NULL OR t.autor.id <> :callerId)")
    Page<TemplateEvaluacion> buscarPublicas(@Param("callerId") Long callerId, Pageable pageable);
}
