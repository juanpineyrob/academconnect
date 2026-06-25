package com.academconnect.service;

import com.academconnect.domain.EstadoInstanciaEvaluacion;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.InstanciaEvaluacion;
import com.academconnect.domain.InstanciaEvaluacionConfig;
import com.academconnect.domain.Trabajo;
import com.academconnect.repository.InstanciaEvaluacionConfigRepository;
import com.academconnect.repository.InstanciaEvaluacionRepository;
import com.academconnect.repository.TipoTrabajoConfigRepository;
import com.academconnect.repository.TrabajoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class InstanciaEvaluacionService {

    private final InstanciaEvaluacionRepository repository;
    private final InstanciaEvaluacionConfigRepository configRepository;
    private final TipoTrabajoConfigRepository tipoTrabajoConfigRepository;
    private final TrabajoRepository trabajoRepository;

    /** Materializa la primera instancia (orden 0) si el tipo tiene config y no hay activa. */
    public Optional<InstanciaEvaluacion> materializarInicial(Trabajo trabajo) {
        List<InstanciaEvaluacionConfig> configs = configRepository.findByTipoOrderByOrden(trabajo.getTipo());
        if (configs.isEmpty()) return Optional.empty();
        if (instanciaActiva(trabajo.getId()).isPresent()) return Optional.empty();
        return Optional.of(materializar(trabajo, configs.get(0), 1));
    }

    @Transactional(readOnly = true)
    public Optional<InstanciaEvaluacion> instanciaActiva(Long trabajoId) {
        return repository.findFirstByTrabajoIdAndEstadoNotInOrderByOrdenAsc(
                trabajoId, List.of(EstadoInstanciaEvaluacion.APROBADA, EstadoInstanciaEvaluacion.REPROBADA));
    }

    public void alAprobar(InstanciaEvaluacion instancia, BigDecimal puntaje) {
        cerrar(instancia, EstadoInstanciaEvaluacion.APROBADA, puntaje);
        var trabajo = instancia.getTrabajo();
        boolean secuencial = tipoTrabajoConfigRepository.findById(trabajo.getTipo())
                .map(c -> c.isSecuencial()).orElse(true);

        List<InstanciaEvaluacionConfig> configs = configRepository.findByTipoOrderByOrden(trabajo.getTipo());
        Optional<InstanciaEvaluacionConfig> siguiente = configs.stream()
                .filter(c -> c.getOrden() > instancia.getOrden())
                .findFirst();

        if (secuencial) {
            if (siguiente.isPresent()) {
                materializar(trabajo, siguiente.get(), 1);
            } else {
                aprobarTrabajo(trabajo);
            }
        } else {
            // independiente: aprobar el trabajo cuando todas las config tengan una instancia APROBADA
            boolean todasAprobadas = configs.stream().allMatch(c ->
                    repository.countByTrabajoIdAndInstanciaConfigIdAndEstado(
                            trabajo.getId(), c.getId(), EstadoInstanciaEvaluacion.APROBADA) > 0);
            if (todasAprobadas) {
                aprobarTrabajo(trabajo);
            } else if (siguiente.isPresent()) {
                materializar(trabajo, siguiente.get(), 1);
            }
        }
    }

    /** Transiciona la instancia de PENDIENTE a EN_CURSO si aún está en ese estado. */
    public void marcarEnCurso(InstanciaEvaluacion instancia) {
        if (instancia.getEstado() == EstadoInstanciaEvaluacion.PENDIENTE) {
            instancia.setEstado(EstadoInstanciaEvaluacion.EN_CURSO);
            repository.save(instancia);
        }
    }

    public void alReprobar(InstanciaEvaluacion instancia, BigDecimal puntaje) {
        cerrar(instancia, EstadoInstanciaEvaluacion.REPROBADA, puntaje);
        var trabajo = instancia.getTrabajo();
        if (instancia.getIntento() < instancia.getInstanciaConfig().getMaxIntentos()) {
            materializar(trabajo, instancia.getInstanciaConfig(), instancia.getIntento() + 1);
        } else {
            rechazarTrabajo(trabajo);
        }
    }

    private InstanciaEvaluacion materializar(Trabajo trabajo, InstanciaEvaluacionConfig config, int intento) {
        var ie = new InstanciaEvaluacion();
        ie.setTrabajo(trabajo);
        ie.setInstanciaConfig(config);
        ie.setOrden(config.getOrden());
        ie.setIntento(intento);
        ie.setEstado(EstadoInstanciaEvaluacion.PENDIENTE);
        return repository.save(ie);
    }

    private void cerrar(InstanciaEvaluacion instancia, EstadoInstanciaEvaluacion estado, BigDecimal puntaje) {
        instancia.setEstado(estado);
        instancia.setPuntajeAgregado(puntaje);
        instancia.setCerradaEn(Instant.now());
        repository.save(instancia);
    }

    private void aprobarTrabajo(Trabajo trabajo) {
        trabajo.setEstado(EstadoTrabajo.APROBADO);
        trabajo.setEvaluadoEn(Instant.now());
        trabajoRepository.save(trabajo);
    }

    private void rechazarTrabajo(Trabajo trabajo) {
        trabajo.setEstado(EstadoTrabajo.RECHAZADO);
        trabajo.setEvaluadoEn(Instant.now());
        trabajoRepository.save(trabajo);
    }
}
