package com.academconnect.repository;

import com.academconnect.domain.EstadoInstanciaEvaluacion;
import com.academconnect.domain.InstanciaEvaluacion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanciaEvaluacionRepository extends JpaRepository<InstanciaEvaluacion, Long> {

    List<InstanciaEvaluacion> findByTrabajoIdOrderByOrdenAscIntentoAsc(Long trabajoId);

    /** Instancia activa: la no cerrada de menor orden. */
    Optional<InstanciaEvaluacion> findFirstByTrabajoIdAndEstadoNotInOrderByOrdenAsc(
            Long trabajoId, Collection<EstadoInstanciaEvaluacion> estados);

    long countByTrabajoIdAndInstanciaConfigIdAndEstado(
            Long trabajoId, Long instanciaConfigId, EstadoInstanciaEvaluacion estado);
}
