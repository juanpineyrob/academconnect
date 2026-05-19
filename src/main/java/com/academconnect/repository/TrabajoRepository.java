package com.academconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.Trabajo;
import com.academconnect.dto.TrabajosPorEstadoDto;

public interface TrabajoRepository extends JpaRepository<Trabajo, Long> {

    List<Trabajo> findByEstudianteIdAndTipoAndEstadoIn(Long estudianteId, TipoTrabajo tipo, List<EstadoTrabajo> estados);

    List<Trabajo> findByOrientadorId(Long orientadorId);

    List<Trabajo> findByEstado(EstadoTrabajo estado);

    @Query("SELECT new com.academconnect.dto.TrabajosPorEstadoDto(t.estado, COUNT(t)) FROM Trabajo t GROUP BY t.estado")
    List<TrabajosPorEstadoDto> contarPorEstado();

    @Query(value = "SELECT * FROM trabajo WHERE search_vector @@ plainto_tsquery('spanish', :q)",
           nativeQuery = true)
    List<Trabajo> buscarPorTexto(@org.springframework.data.repository.query.Param("q") String q);
}
