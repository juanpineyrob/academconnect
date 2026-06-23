package com.academconnect.domain;

/** Estado de una solicitud de cuenta (self-request) en la cola del admin. */
public enum EstadoSolicitudCuenta {
    PENDIENTE,
    APROBADA,
    RECHAZADA
}
