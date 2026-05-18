package com.academconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.CalificacionCriterio;

public interface CalificacionCriterioRepository extends JpaRepository<CalificacionCriterio, Long> {

    List<CalificacionCriterio> findByEvaluacionId(Long evaluacionId);
}
