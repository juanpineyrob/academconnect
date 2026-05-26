package com.academconnect.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.TipoTrabajoConfig;
import com.academconnect.dto.TipoTrabajoConfigRequest;
import com.academconnect.dto.TipoTrabajoConfigResponse;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.TipoTrabajoConfigRepository;

import lombok.RequiredArgsConstructor;

/** F14 — admin gestiona la configuración de modo de evaluación y default de evaluadores por tipo. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TipoTrabajoConfigService {

    private final TipoTrabajoConfigRepository repository;

    public List<TipoTrabajoConfigResponse> listar() {
        return repository.findAll().stream().map(TipoTrabajoConfigService::toResponse).toList();
    }

    public TipoTrabajoConfigResponse buscarPorTipo(TipoTrabajo tipo) {
        return repository.findById(tipo).map(TipoTrabajoConfigService::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("TipoTrabajoConfig", tipo));
    }

    @Transactional
    public TipoTrabajoConfigResponse actualizar(TipoTrabajo tipo, TipoTrabajoConfigRequest request) {
        var config = repository.findById(tipo).orElseGet(() -> {
            var nueva = new TipoTrabajoConfig();
            nueva.setTipo(tipo);
            return nueva;
        });
        config.setModoEvaluacion(request.modoEvaluacion());
        config.setEvaluadoresDefault(request.evaluadoresDefault());
        return toResponse(repository.save(config));
    }

    private static TipoTrabajoConfigResponse toResponse(TipoTrabajoConfig c) {
        return new TipoTrabajoConfigResponse(c.getTipo(), c.getModoEvaluacion(), c.getEvaluadoresDefault());
    }
}
