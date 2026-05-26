package com.academconnect.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academconnect.domain.Actividad;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    /**
     * Feed personalizado: el usuario ve eventos donde es actor, eventos públicos, o eventos
     * con visibilidad PARTICIPANTES en los que figura en participantes_ids.
     */
    @Query(value = """
            SELECT * FROM actividad a
            WHERE a.actor_id = :userId
               OR a.visibilidad = 'PUBLICA'
               OR (a.visibilidad = 'PARTICIPANTES' AND :userId = ANY(a.participantes_ids))
            ORDER BY a.created_at DESC
            """, nativeQuery = true)
    List<Actividad> feedDelUsuario(@Param("userId") Long userId, Pageable pageable);
}
