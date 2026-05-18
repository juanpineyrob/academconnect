CREATE TABLE documento (
    id BIGSERIAL PRIMARY KEY,
    storage_key VARCHAR(500) NOT NULL,
    nombre_original VARCHAR(300) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT uq_documento_storage_key UNIQUE (storage_key),
    CONSTRAINT chk_documento_size CHECK (size_bytes > 0)
);

CREATE INDEX idx_documento_sha256 ON documento(sha256);

CREATE TABLE versionamiento (
    id BIGSERIAL PRIMARY KEY,
    trabajo_id BIGINT NOT NULL REFERENCES trabajo(id) ON DELETE CASCADE,
    numero_version INTEGER NOT NULL,
    comentario TEXT,
    documento_id BIGINT NOT NULL REFERENCES documento(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT uq_versionamiento_trabajo_numero UNIQUE (trabajo_id, numero_version),
    CONSTRAINT chk_versionamiento_numero CHECK (numero_version > 0)
);

CREATE INDEX idx_versionamiento_trabajo ON versionamiento(trabajo_id);
CREATE INDEX idx_versionamiento_documento ON versionamiento(documento_id);
