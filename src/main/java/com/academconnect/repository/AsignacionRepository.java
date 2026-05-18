package com.academconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.Asignacion;
import com.academconnect.domain.EstadoAsignacion;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    List<Asignacion> findByTrabajoId(Long trabajoId);

    List<Asignacion> findByEvaluadorIdAndEstado(Long evaluadorId, EstadoAsignacion estado);

    long countByEvaluadorIdAndEstado(Long evaluadorId, EstadoAsignacion estado);
}
