-- V30__instancia_evaluacion.sql
CREATE TABLE instancia_evaluacion (
    id BIGSERIAL PRIMARY KEY,
    trabajo_id BIGINT NOT NULL REFERENCES trabajo(id) ON DELETE CASCADE,
    instancia_config_id BIGINT NOT NULL REFERENCES instancia_evaluacion_config(id) ON DELETE RESTRICT,
    orden INTEGER NOT NULL,
    intento INTEGER NOT NULL,
    estado VARCHAR(20) NOT NULL,
    puntaje_agregado NUMERIC(6,2),
    cerrada_en TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_instancia_eval_estado CHECK (
        estado IN ('PENDIENTE','EN_CURSO','APROBADA','REPROBADA')
    ),
    CONSTRAINT chk_instancia_eval_intento CHECK (intento >= 1)
);

CREATE INDEX ix_instancia_eval_trabajo ON instancia_evaluacion (trabajo_id);

-- A lo sumo una instancia ABIERTA (PENDIENTE/EN_CURSO) por trabajo+config (evita doble materialización).
CREATE UNIQUE INDEX uq_instancia_eval_abierta
    ON instancia_evaluacion (trabajo_id, instancia_config_id)
    WHERE estado IN ('PENDIENTE','EN_CURSO');

ALTER TABLE asignacion
    ADD COLUMN instancia_evaluacion_id BIGINT REFERENCES instancia_evaluacion(id) ON DELETE SET NULL;
