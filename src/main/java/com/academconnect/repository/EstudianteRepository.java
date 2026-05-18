package com.academconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.Estudiante;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
}
