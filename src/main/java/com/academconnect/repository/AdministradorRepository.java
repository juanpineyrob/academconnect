package com.academconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.Administrador;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
}
