package com.academconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.Trabajo;

public interface TrabajoRepository extends JpaRepository<Trabajo, Long> {

    List<Trabajo> findByEstudianteIdAndTipoAndEstadoIn(Long estudianteId, TipoTrabajo tipo, List<EstadoTrabajo> estados);

    List<Trabajo> findByOrientadorId(Long orientadorId);

    List<Trabajo> findByEstado(EstadoTrabajo estado);
}
