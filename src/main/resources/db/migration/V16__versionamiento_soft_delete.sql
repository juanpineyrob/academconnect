ALTER TABLE versionamiento
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN deleted_by VARCHAR(100);

CREATE INDEX idx_versionamiento_trabajo_activas
    ON versionamiento(trabajo_id)
    WHERE deleted_at IS NULL;
