package com.academconnect.repository;

import com.academconnect.domain.InstanciaEvaluacionConfig;
import com.academconnect.domain.TipoTrabajo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanciaEvaluacionConfigRepository
        extends JpaRepository<InstanciaEvaluacionConfig, Long> {

    List<InstanciaEvaluacionConfig> findByTipoOrderByOrden(TipoTrabajo tipo);

    void deleteByTipo(TipoTrabajo tipo);
}
