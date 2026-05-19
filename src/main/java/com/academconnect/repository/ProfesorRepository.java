package com.academconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.Profesor;

public interface ProfesorRepository extends JpaRepository<Profesor, Long> {

    List<Profesor> findByActivo(boolean activo);
}
