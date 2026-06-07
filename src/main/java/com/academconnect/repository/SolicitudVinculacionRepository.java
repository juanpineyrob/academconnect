package com.academconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.academconnect.domain.EstadoSolicitud;
import com.academconnect.domain.SolicitudVinculacion;

public interface SolicitudVinculacionRepository extends JpaRepository<SolicitudVinculacion, Long> {

    List<SolicitudVinculacion> findByTrabajoId(Long trabajoId);

    List<SolicitudVinculacion> findByTrabajoIdAndEstado(Long trabajoId, EstadoSolicitud estado);

    List<SolicitudVinculacion> findByEstudianteId(Long estudianteId);

    boolean existsByTrabajoIdAndEstudianteIdAndEstado(Long trabajoId, Long estudianteId, EstadoSolicitud estado);

    List<SolicitudVinculacion> findByEstudianteIdOrderByCreatedAtDesc(Long estudianteId);

    @Query(
        "SELECT s FROM SolicitudVinculacion s WHERE s.trabajo.orientador.id = :profesorId ORDER BY s.createdAt DESC")
    List<SolicitudVinculacion> findRecibidasPorProfesor(@org.springframework.data.repository.query.Param("profesorId") Long profesorId);
}
