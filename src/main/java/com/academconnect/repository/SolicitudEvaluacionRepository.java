package com.academconnect.repository;

import com.academconnect.domain.EstadoInvitacion;
import com.academconnect.domain.SolicitudEvaluacion;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudEvaluacionRepository extends JpaRepository<SolicitudEvaluacion, Long> {

    boolean existsByTrabajoIdAndInvitadoIdAndEstado(Long trabajoId, Long invitadoId, EstadoInvitacion estado);

    long countByTrabajoIdAndEstado(Long trabajoId, EstadoInvitacion estado);

    List<SolicitudEvaluacion> findByTrabajoIdOrderByCreatedAtDesc(Long trabajoId);

    Page<SolicitudEvaluacion> findByInvitadoIdAndEstadoOrderByCreatedAtDesc(
            Long invitadoId, EstadoInvitacion estado, Pageable pageable);

    Page<SolicitudEvaluacion> findByInvitadoIdAndEstadoNotOrderByCreatedAtDesc(
            Long invitadoId, EstadoInvitacion estado, Pageable pageable);
}
