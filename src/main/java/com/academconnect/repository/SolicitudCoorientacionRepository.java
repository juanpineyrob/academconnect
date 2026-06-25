package com.academconnect.repository;

import com.academconnect.domain.EstadoInvitacion;
import com.academconnect.domain.SolicitudCoorientacion;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudCoorientacionRepository extends JpaRepository<SolicitudCoorientacion, Long> {

    boolean existsByTrabajoIdAndEstado(Long trabajoId, EstadoInvitacion estado);

    List<SolicitudCoorientacion> findByTrabajoIdOrderByCreatedAtDesc(Long trabajoId);

    Page<SolicitudCoorientacion> findByInvitadoIdAndEstadoOrderByCreatedAtDesc(
            Long invitadoId, EstadoInvitacion estado, Pageable pageable);

    Page<SolicitudCoorientacion> findByInvitadoIdAndEstadoNotOrderByCreatedAtDesc(
            Long invitadoId, EstadoInvitacion estado, Pageable pageable);
}
