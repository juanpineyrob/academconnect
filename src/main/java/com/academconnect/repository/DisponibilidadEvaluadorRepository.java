package com.academconnect.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academconnect.domain.DisponibilidadEvaluador;

public interface DisponibilidadEvaluadorRepository extends JpaRepository<DisponibilidadEvaluador, Long> {

    List<DisponibilidadEvaluador> findByEvaluadorIdAndFechaBetween(
            Long evaluadorId, LocalDate desde, LocalDate hasta);

    Optional<DisponibilidadEvaluador> findByEvaluadorIdAndFecha(Long evaluadorId, LocalDate fecha);

    /** Total de horas declaradas en el rango (proxy de "disponibilidad semanal"). */
    @Query("SELECT COALESCE(SUM(d.horasDisponibles), 0) FROM DisponibilidadEvaluador d " +
           "WHERE d.evaluador.id = :evaluadorId AND d.fecha BETWEEN :desde AND :hasta")
    BigDecimal totalHoras(@Param("evaluadorId") Long evaluadorId,
                          @Param("desde") LocalDate desde,
                          @Param("hasta") LocalDate hasta);
}
