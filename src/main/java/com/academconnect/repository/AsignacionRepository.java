package com.academconnect.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.academconnect.domain.Asignacion;
import com.academconnect.domain.EstadoAsignacion;
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
}
