package com.academconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.Externo;

public interface ExternoRepository extends JpaRepository<Externo, Long> {

    List<Externo> findByActivo(boolean activo);
}
