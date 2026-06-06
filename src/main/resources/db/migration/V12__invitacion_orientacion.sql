-- V12__invitacion_orientacion.sql

-- Camino 2.1 — el estudiante crea borradores sin orientador hasta vincular vía invitación.
ALTER TABLE trabajo ALTER COLUMN orientador_id DROP NOT NULL;

CREATE TABLE invitacion_orientacion (
    id BIGSERIAL PRIMARY KEY,
    trabajo_id BIGINT NOT NULL REFERENCES trabajo(id) ON DELETE CASCADE,
    profesor_id BIGINT NOT NULL REFERENCES profesor(id) ON DELETE RESTRICT,
    estado VARCHAR(40) NOT NULL,
    motivo TEXT,
    respuesta TEXT,
    resuelta_en TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_invitacion_estado CHECK (
        estado IN ('PENDIENTE','ACEPTADA','RECHAZADA','CANCELADA')
    )
);

CREATE INDEX ix_invitacion_trabajo ON invitacion_orientacion (trabajo_id);
CREATE INDEX ix_invitacion_profesor_estado ON invitacion_orientacion (profesor_id, estado);

-- Una sola invitación pendiente por trabajo (cardinalidad secuencial).
CREATE UNIQUE INDEX uq_invitacion_pendiente_por_trabajo
    ON invitacion_orientacion (trabajo_id)
    WHERE estado = 'PENDIENTE';
