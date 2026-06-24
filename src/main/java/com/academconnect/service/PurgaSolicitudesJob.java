package com.academconnect.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.EstadoLote;
import com.academconnect.domain.EstadoSolicitudCuenta;
import com.academconnect.repository.LoteImportacionRepository;
import com.academconnect.repository.SolicitudCuentaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Retención de PII: borra periódicamente la PII de solicitudes no aprobadas y
 * de lotes de importación nunca confirmados.
 * El metadato de decisión (rechazo/aprobación) ya persiste en {@code actividad}
 * sin PII, por lo que borrar la fila de {@link com.academconnect.domain.SolicitudCuenta}
 * solo elimina la PII conservando la auditoría.
 */
@Service
@RequiredArgsConstructor
public class PurgaSolicitudesJob {

    private final SolicitudCuentaRepository solicitudRepository;
    private final LoteImportacionRepository loteRepository;

    @Scheduled(cron = "${academconnect.onboarding.purga-cron:0 0 3 * * *}")
    public void purgarProgramado() {
        purgar(Instant.now());
    }

    /** Borra solicitudes PENDIENTE/RECHAZADA >7d y lotes PREVIEW >24h (items por ON DELETE CASCADE). */
    @Transactional
    public void purgar(Instant ahora) {
        var solicitudes = solicitudRepository.findByEstadoInAndUpdatedAtBefore(
                List.of(EstadoSolicitudCuenta.PENDIENTE, EstadoSolicitudCuenta.RECHAZADA),
                ahora.minus(7, ChronoUnit.DAYS));
        solicitudRepository.deleteAll(solicitudes);

        var lotes = loteRepository.findByEstadoAndCreatedAtBefore(
                EstadoLote.PREVIEW, ahora.minus(24, ChronoUnit.HOURS));
        loteRepository.deleteAll(lotes);
    }
}
