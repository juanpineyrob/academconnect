package com.academconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.Coorientador;

public interface CoorientadorRepository extends JpaRepository<Coorientador, Long> {

    List<Coorientador> findByTrabajoId(Long trabajoId);

    List<Coorientador> findByUsuarioId(Long usuarioId);

    long countByTrabajoId(Long trabajoId);
}
