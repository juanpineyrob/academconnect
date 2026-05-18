package com.academconnect.domain;

import java.util.Set;

public enum EstadoTrabajo {
    BORRADOR,
    ABIERTO,
    EN_DESARROLLO,
    EN_EVALUACION,
    APROBADO,
    RECHAZADO,
    CANCELADO;

    private static final Set<EstadoTrabajo> FINALIZADOS = Set.of(APROBADO, RECHAZADO, CANCELADO);

    public boolean esFinalizado() {
        return FINALIZADOS.contains(this);
    }

    public boolean esActivo() {
        return !esFinalizado();
    }
}
