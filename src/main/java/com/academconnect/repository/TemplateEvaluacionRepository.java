package com.academconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.TemplateEvaluacion;
import com.academconnect.domain.TemplateScope;
import com.academconnect.domain.TipoTrabajo;

public interface TemplateEvaluacionRepository extends JpaRepository<TemplateEvaluacion, Long> {

    List<TemplateEvaluacion> findByActivoTrue();

    List<TemplateEvaluacion> findByScopeAndActivoTrue(TemplateScope scope);

    List<TemplateEvaluacion> findByTipoTrabajoAplicableAndActivoTrue(TipoTrabajo tipo);
}
