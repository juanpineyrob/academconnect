-- V25__solicitud_coorientacion.sql

-- Solicitud de coorientador: el estudiante invita a un profesor o externo (usuario)
-- a coorientar un trabajo que ya tiene orientador. Flujo invitación + aceptar/rechazar.
CREATE TABLE solicitud_coorientacion (
    id BIGSERIAL PRIMARY KEY,
    trabajo_id BIGINT NOT NULL REFERENCES trabajo(id) ON DELETE CASCADE,
    invitado_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    estado VARCHAR(40) NOT NULL,
    motivo TEXT,
    respuesta TEXT,
    resuelta_en TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_solicitud_coorientacion_estado CHECK (
        estado IN ('PENDIENTE','ACEPTADA','RECHAZADA','CANCELADA')
    )
);

CREATE INDEX ix_solicitud_coorientacion_trabajo ON solicitud_coorientacion (trabajo_id);
CREATE INDEX ix_solicitud_coorientacion_invitado_estado ON solicitud_coorientacion (invitado_id, estado);

-- Una sola solicitud pendiente por trabajo.
CREATE UNIQUE INDEX uq_solicitud_coorientacion_pendiente_por_trabajo
    ON solicitud_coorientacion (trabajo_id)
    WHERE estado = 'PENDIENTE';

-- Regla de negocio: como máximo un coorientador por trabajo (cierra la ventana TOCTOU
-- en aceptar(), donde dos solicitudes secuenciales podrían crear dos coorientadores).
CREATE UNIQUE INDEX uq_coorientador_un_por_trabajo
    ON coorientador (trabajo_id);
