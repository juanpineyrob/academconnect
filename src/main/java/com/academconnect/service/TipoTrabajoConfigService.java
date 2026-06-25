package com.academconnect.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.InstanciaEvaluacionConfig;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.TipoTrabajoConfig;
import com.academconnect.dto.InstanciaEvaluacionConfigDto;
import com.academconnect.dto.InstanciaEvaluacionConfigInput;
import com.academconnect.dto.TipoTrabajoConfigRequest;
import com.academconnect.dto.TipoTrabajoConfigResponse;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.InstanciaEvaluacionConfigRepository;
import com.academconnect.repository.TipoTrabajoConfigRepository;

import lombok.RequiredArgsConstructor;

/** F14 / 4a — admin gestiona modo, default de evaluadores y estructura de instancias por tipo. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TipoTrabajoConfigService {

    private final TipoTrabajoConfigRepository repository;
    private final InstanciaEvaluacionConfigRepository instanciaRepository;

    public List<TipoTrabajoConfigResponse> listar() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public TipoTrabajoConfigResponse buscarPorTipo(TipoTrabajo tipo) {
        return repository.findById(tipo).map(this::toResponse)
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
        config.setSecuencial(request.secuencial());
        var savedConfig = repository.save(config);

        instanciaRepository.deleteByTipo(tipo);
        instanciaRepository.flush();
        List<InstanciaEvaluacionConfigInput> entradas =
                request.instancias() == null ? List.of() : request.instancias();
        List<InstanciaEvaluacionConfig> nuevas = new java.util.ArrayList<>();
        for (int i = 0; i < entradas.size(); i++) {
            var in = entradas.get(i);
            var inst = new InstanciaEvaluacionConfig();
            inst.setTipo(tipo);
            inst.setOrden(i);
            inst.setNombre(in.nombre());
            inst.setEvaluadoresRequeridos(in.evaluadoresRequeridos());
            inst.setMaxIntentos(in.maxIntentos());
            nuevas.add(inst);
        }
        instanciaRepository.saveAll(nuevas);

        return toResponse(savedConfig);
    }

    private TipoTrabajoConfigResponse toResponse(TipoTrabajoConfig c) {
        List<InstanciaEvaluacionConfigDto> instancias =
                instanciaRepository.findByTipoOrderByOrden(c.getTipo()).stream()
                        .map(i -> new InstanciaEvaluacionConfigDto(
                                i.getOrden(), i.getNombre(), i.getEvaluadoresRequeridos(), i.getMaxIntentos()))
                        .toList();
        return new TipoTrabajoConfigResponse(
                c.getTipo(), c.getModoEvaluacion(), c.getEvaluadoresDefault(), instancias, c.isSecuencial());
    }
}
