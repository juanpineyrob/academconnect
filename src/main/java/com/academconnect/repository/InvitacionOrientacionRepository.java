package com.academconnect.repository;

import com.academconnect.domain.EstadoInvitacion;
import com.academconnect.domain.InvitacionOrientacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitacionOrientacionRepository extends JpaRepository<InvitacionOrientacion, Long> {

    List<InvitacionOrientacion> findByTrabajoIdOrderByCreatedAtDesc(Long trabajoId);

    List<InvitacionOrientacion> findByProfesorIdAndEstadoOrderByCreatedAtDesc(
            Long profesorId, EstadoInvitacion estado);

    List<InvitacionOrientacion> findByProfesorIdOrderByCreatedAtDesc(Long profesorId);

    Optional<InvitacionOrientacion> findFirstByTrabajoIdAndEstado(Long trabajoId, EstadoInvitacion estado);

    boolean existsByTrabajoIdAndEstado(Long trabajoId, EstadoInvitacion estado);
}
