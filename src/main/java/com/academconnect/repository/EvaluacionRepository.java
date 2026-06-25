package com.academconnect.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academconnect.domain.EstadoEvaluacion;
import com.academconnect.domain.Evaluacion;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    Optional<Evaluacion> findByAsignacionId(Long asignacionId);

    List<Evaluacion> findByAsignacionTrabajoIdAndEstado(Long trabajoId, EstadoEvaluacion estado);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (e.completada_en - a.asignada_en)) / 3600.0) " +
                   "FROM evaluacion e JOIN asignacion a ON a.id = e.asignacion_id " +
                   "WHERE e.estado = 'COMPLETADA'",
           nativeQuery = true)
    Double promedioTiempoEvaluacionHoras();

    /** Promedio de calificacion_final de evaluaciones COMPLETADA para un trabajo y una versión. */
    @Query("SELECT AVG(e.calificacionFinal) FROM Evaluacion e " +
           "WHERE e.asignacion.trabajo.id = :trabajoId " +
           "AND e.asignacion.versionamiento.id = :versionamientoId " +
           "AND e.estado = com.academconnect.domain.EstadoEvaluacion.COMPLETADA")
    BigDecimal promedioPorTrabajoYVersion(
            @Param("trabajoId") Long trabajoId,
            @Param("versionamientoId") Long versionamientoId);

    /** Promedio de calificacion_final de evaluaciones COMPLETADA para una instancia de evaluación. */
    @Query("SELECT AVG(e.calificacionFinal) FROM Evaluacion e " +
           "WHERE e.asignacion.instanciaEvaluacion.id = :instanciaId " +
           "AND e.estado = com.academconnect.domain.EstadoEvaluacion.COMPLETADA")
    BigDecimal promedioPorInstancia(@Param("instanciaId") Long instanciaId);

    @Query("SELECT COUNT(e) FROM Evaluacion e " +
           "WHERE e.asignacion.evaluador.id = :evaluadorId " +
           "AND e.estado = com.academconnect.domain.EstadoEvaluacion.COMPLETADA")
    long countCompletadasPorEvaluador(@Param("evaluadorId") Long evaluadorId);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (e.completada_en - a.asignada_en)) / 3600.0) " +
                   "FROM evaluacion e JOIN asignacion a ON a.id = e.asignacion_id " +
                   "WHERE a.evaluador_id = :evaluadorId AND e.estado = 'COMPLETADA'",
           nativeQuery = true)
    Double tiempoMedioHorasPorEvaluador(@Param("evaluadorId") Long evaluadorId);

    @Query("SELECT AVG(e.calificacionFinal) FROM Evaluacion e " +
           "WHERE e.asignacion.evaluador.id = :evaluadorId " +
           "AND e.estado = com.academconnect.domain.EstadoEvaluacion.COMPLETADA")
    BigDecimal scoreMedioPorEvaluador(@Param("evaluadorId") Long evaluadorId);
}
