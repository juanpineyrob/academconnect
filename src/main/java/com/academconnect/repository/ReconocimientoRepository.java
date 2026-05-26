package com.academconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.Reconocimiento;

public interface ReconocimientoRepository extends JpaRepository<Reconocimiento, Long> {

    List<Reconocimiento> findByUsuarioIdOrderByAnioDesc(Long usuarioId);
}
