-- F15 — Feed de actividad (G06). Event sourcing ligero alimentado por ApplicationEventPublisher.

CREATE TABLE actividad (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(60) NOT NULL,
    actor_id BIGINT REFERENCES usuario(id) ON DELETE SET NULL,
    recurso_tipo VARCHAR(40) NOT NULL,
    recurso_id BIGINT NOT NULL,
    payload JSONB NOT NULL,
    visibilidad VARCHAR(20) NOT NULL,
    participantes_ids BIGINT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_actividad_visibilidad CHECK (visibilidad IN ('PRIVADA','PARTICIPANTES','PUBLICA'))
);

CREATE INDEX idx_actividad_recurso ON actividad (recurso_tipo, recurso_id);
CREATE INDEX idx_actividad_actor ON actividad (actor_id, created_at DESC);
CREATE INDEX idx_actividad_created ON actividad (created_at DESC);
CREATE INDEX idx_actividad_participantes ON actividad USING GIN (participantes_ids);
