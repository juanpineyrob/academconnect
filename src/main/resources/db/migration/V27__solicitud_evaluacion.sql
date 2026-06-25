-- V27__solicitud_evaluacion.sql

-- Solicitud de evaluación: el estudiante invita a un evaluador (profesor o externo).
-- Al aceptar se crea la Asignacion. Flujo invitación + aceptar/rechazar.
CREATE TABLE solicitud_evaluacion (
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
    CONSTRAINT chk_solicitud_evaluacion_estado CHECK (
        estado IN ('PENDIENTE','ACEPTADA','RECHAZADA','CANCELADA')
    )
);

CREATE INDEX ix_solicitud_evaluacion_trabajo ON solicitud_evaluacion (trabajo_id);
CREATE INDEX ix_solicitud_evaluacion_invitado_estado ON solicitud_evaluacion (invitado_id, estado);

-- No invitar dos veces al mismo evaluador con una solicitud pendiente.
CREATE UNIQUE INDEX uq_solicitud_evaluacion_pendiente_invitado
    ON solicitud_evaluacion (trabajo_id, invitado_id)
    WHERE estado = 'PENDIENTE';
