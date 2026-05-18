package com.academconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.ConflictoInteres;

public interface ConflictoInteresRepository extends JpaRepository<ConflictoInteres, Long> {

    List<ConflictoInteres> findByTrabajoId(Long trabajoId);

    boolean existsByTrabajoIdAndEvaluadorId(Long trabajoId, Long evaluadorId);
}
