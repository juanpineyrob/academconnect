package com.academconnect.event;

import java.util.List;
import java.util.Map;

import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.VisibilidadActividad;

/**
 * F15 — evento de dominio publicado por services al ocurrir un cambio relevante.
 * Un listener async lo persiste en la tabla {@code actividad}.
 */
public record ActividadEvent(
        TipoActividad tipo,
        Long actorId,
        String recursoTipo,
        Long recursoId,
        Map<String, Object> payload,
        VisibilidadActividad visibilidad,
        List<Long> participantesIds) {

    public static ActividadEvent of(
            TipoActividad tipo, Long actorId, String recursoTipo, Long recursoId,
            Map<String, Object> payload, VisibilidadActividad visibilidad, List<Long> participantesIds) {
        return new ActividadEvent(tipo, actorId, recursoTipo, recursoId,
                payload == null ? Map.of() : payload,
                visibilidad,
                participantesIds == null ? List.of() : participantesIds);
    }
}
