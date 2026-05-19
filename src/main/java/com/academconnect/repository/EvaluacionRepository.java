package com.academconnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.EstadoEvaluacion;
import com.academconnect.domain.Evaluacion;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    Optional<Evaluacion> findByAsignacionId(Long asignacionId);

    List<Evaluacion> findByAsignacionTrabajoIdAndEstado(Long trabajoId, EstadoEvaluacion estado);
}
