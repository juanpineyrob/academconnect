package com.academconnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.EstadoEvaluacion;
import com.academconnect.domain.Evaluacion;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    Optional<Evaluacion> findByAsignacionId(Long asignacionId);

    List<Evaluacion> findByAsignacionTrabajoIdAndEstado(Long trabajoId, EstadoEvaluacion estado);

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT AVG(EXTRACT(EPOCH FROM (e.completada_en - a.asignada_en)) / 3600.0) " +
                    "FROM evaluacion e JOIN asignacion a ON a.id = e.asignacion_id " +
                    "WHERE e.estado = 'COMPLETADA'",
            nativeQuery = true)
    Double promedioTiempoEvaluacionHoras();
}
