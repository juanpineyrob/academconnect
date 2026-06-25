-- V28__instancia_evaluacion_config.sql

-- Estructura de instancias de evaluación por tipo de trabajo (4a).
-- Cada tipo tiene una lista ordenada de instancias, cada una con N evaluadores.
CREATE TABLE instancia_evaluacion_config (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(50) NOT NULL,
    orden INTEGER NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    evaluadores_requeridos INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_instancia_tipo CHECK (
        tipo IN ('TCC','TESIS','PAPER','MONOGRAFIA','PROYECTO_INVESTIGACION')
    ),
    CONSTRAINT chk_instancia_evaluadores CHECK (evaluadores_requeridos >= 1),
    CONSTRAINT uq_instancia_tipo_orden UNIQUE (tipo, orden)
);

CREATE INDEX ix_instancia_tipo ON instancia_evaluacion_config (tipo);

-- Seed: TCC = 2 instancias × 2 evaluadores (proceso real de la facultad).
INSERT INTO instancia_evaluacion_config
    (tipo, orden, nombre, evaluadores_requeridos, created_at, updated_at, created_by, updated_by)
VALUES
    ('TCC', 0, 'TCC1', 2, now(), now(), 'system', 'system'),
    ('TCC', 1, 'TCC2', 2, now(), now(), 'system', 'system');
