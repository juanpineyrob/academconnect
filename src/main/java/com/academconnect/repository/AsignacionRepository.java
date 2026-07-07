package com.academconnect.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.academconnect.domain.Asignacion;
import com.academconnect.domain.EstadoAsignacion;
import com.academconnect.dto.AsignacionExportRow;
import com.academconnect.dto.CargaEvaluadorDto;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    List<Asignacion> findByTrabajoId(Long trabajoId);

    List<Asignacion> findByEvaluadorIdAndEstado(Long evaluadorId, EstadoAsignacion estado);

    List<Asignacion> findByEvaluadorId(Long evaluadorId);

    Page<Asignacion> findByEvaluadorIdAndEstado(Long evaluadorId, EstadoAsignacion estado, Pageable pageable);

    Page<Asignacion> findByEvaluadorId(Long evaluadorId, Pageable pageable);

    long countByEvaluadorIdAndEstado(Long evaluadorId, EstadoAsignacion estado);

    long countByTrabajoIdAndEstado(Long trabajoId, EstadoAsignacion estado);

    long countByTrabajoIdAndVersionamientoIdAndEstado(
            Long trabajoId, Long versionamientoId, EstadoAsignacion estado);

    long countByInstanciaEvaluacionIdAndEstado(Long instanciaEvaluacionId, EstadoAsignacion estado);

    @Query("SELECT new com.academconnect.dto.CargaEvaluadorDto(a.evaluador.id, a.evaluador.nombre, COUNT(a)) " +
           "FROM Asignacion a WHERE a.estado = com.academconnect.domain.EstadoAsignacion.ACTIVA " +
           "GROUP BY a.evaluador.id, a.evaluador.nombre " +
           "ORDER BY COUNT(a) DESC")
    List<CargaEvaluadorDto> cargaActivaPorEvaluador();

    /** Detalle crudo (una fila por asignación) para el export CSV de métricas. */
    @Query(value = """
            SELECT
                t.titulo AS trabajoTitulo,
                t.tipo AS trabajoTipo,
                (SELECT string_agg(at.nombre, ', ' ORDER BY at.nombre)
                   FROM trabajo_area_tematica tat
                   JOIN area_tematica at ON at.id = tat.area_id
                  WHERE tat.trabajo_id = t.id) AS areas,
                iec.nombre AS instanciaNombre,
                u.nombre AS evaluadorNombre,
                a.estado AS estadoAsignacion,
                a.asignada_en AS asignadaEn,
                a.vencimiento_en AS vencimientoEn,
                e.calificacion_final AS calificacionFinal,
                e.estado AS estadoEvaluacion
            FROM asignacion a
            JOIN trabajo t ON t.id = a.trabajo_id
            JOIN usuario u ON u.id = a.evaluador_id
            LEFT JOIN instancia_evaluacion ie ON ie.id = a.instancia_evaluacion_id
            LEFT JOIN instancia_evaluacion_config iec ON iec.id = ie.instancia_config_id
            LEFT JOIN evaluacion e ON e.asignacion_id = a.id
            ORDER BY a.asignada_en DESC
            """, nativeQuery = true)
    List<AsignacionExportRow> exportarFilas();
}
