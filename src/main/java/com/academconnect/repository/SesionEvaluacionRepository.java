package com.academconnect.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.EstadoSesionEvaluacion;
import com.academconnect.domain.SesionEvaluacion;

public interface SesionEvaluacionRepository extends JpaRepository<SesionEvaluacion, Long> {

    List<SesionEvaluacion> findByTrabajoId(Long trabajoId);

    List<SesionEvaluacion> findByEstado(EstadoSesionEvaluacion estado);

    List<SesionEvaluacion> findByFechaProgramadaBetween(Instant desde, Instant hasta);
}
