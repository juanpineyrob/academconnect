package com.academconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.TipoTrabajoConfig;

public interface TipoTrabajoConfigRepository extends JpaRepository<TipoTrabajoConfig, TipoTrabajo> {
}
