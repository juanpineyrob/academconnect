package com.academconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.EstadoSolicitud;
import com.academconnect.domain.SolicitudVinculacion;

public interface SolicitudVinculacionRepository extends JpaRepository<SolicitudVinculacion, Long> {

    List<SolicitudVinculacion> findByTrabajoIdAndEstado(Long trabajoId, EstadoSolicitud estado);

    List<SolicitudVinculacion> findByEstudianteId(Long estudianteId);

    boolean existsByTrabajoIdAndEstudianteIdAndEstado(Long trabajoId, Long estudianteId, EstadoSolicitud estado);
}
