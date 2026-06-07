package com.academconnect.job;

import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.Trabajo;
import com.academconnect.domain.VisibilidadActividad;
import com.academconnect.event.ActividadEvent;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.service.TrabajoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Camino 2.2 — cierra trabajos ABIERTO cuya expira_en pasó. */
@Component
@RequiredArgsConstructor
public class TrabajoExpiracionJob {

    private final TrabajoRepository trabajoRepository;
    private final TrabajoService trabajoService;
    private final ApplicationEventPublisher events;

    /** 3am UTC diario. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void expirarVencidos() {
        Instant now = Instant.now();
        List<Trabajo> vencidos = trabajoRepository.findByEstadoAndExpiraEnBefore(EstadoTrabajo.ABIERTO, now);
        for (Trabajo t : vencidos) {
            t.setEstado(EstadoTrabajo.CANCELADO);
            trabajoService.autoRechazarPendientes(t, "Trabajo expirado");
            trabajoRepository.save(t);

            List<Long> participantes = new ArrayList<>();
            if (t.getOrientador() != null) participantes.add(t.getOrientador().getId());
            if (t.getEstudiante() != null) participantes.add(t.getEstudiante().getId());

            events.publishEvent(ActividadEvent.of(
                    TipoActividad.TRABAJO_EXPIRADO,
                    null,
                    "TRABAJO", t.getId(),
                    Map.of("titulo", t.getTitulo()),
                    VisibilidadActividad.PARTICIPANTES,
                    participantes));
        }
    }
}
