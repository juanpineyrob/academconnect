package com.academconnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.Versionamiento;

public interface VersionamientoRepository extends JpaRepository<Versionamiento, Long> {

    List<Versionamiento> findByTrabajoIdOrderByNumeroVersionDesc(Long trabajoId);

    Optional<Versionamiento> findByTrabajoIdAndNumeroVersion(Long trabajoId, int numeroVersion);

    Optional<Versionamiento> findFirstByTrabajoIdOrderByNumeroVersionDesc(Long trabajoId);
}
