package com.academconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.RecomendacionEvaluador;

public interface RecomendacionEvaluadorRepository extends JpaRepository<RecomendacionEvaluador, Long> {

    List<RecomendacionEvaluador> findByTrabajoIdOrderByScoreDesc(Long trabajoId);
}
