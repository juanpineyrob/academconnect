-- Importación masiva de estudiantes: preview/dry-run (PREVIEW) y commit (CONFIRMADO).
-- El preview persiste el lote y sus items ya clasificados sin crear usuarios; el commit
-- crea las cuentas NUEVO como INVITADA y las vincula al lote (usuario.lote_importacion_id).
CREATE TABLE lote_importacion (
    id BIGSERIAL PRIMARY KEY,
    archivo_hash VARCHAR(64) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PREVIEW',
    total INT NOT NULL DEFAULT 0,
    nuevos INT NOT NULL DEFAULT 0,
    existentes INT NOT NULL DEFAULT 0,
    errores INT NOT NULL DEFAULT 0,
    creado_por_id BIGINT REFERENCES usuario(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_lote_estado CHECK (estado IN ('PREVIEW','CONFIRMADO'))
);

CREATE TABLE lote_importacion_item (
    id BIGSERIAL PRIMARY KEY,
    lote_id BIGINT NOT NULL REFERENCES lote_importacion(id) ON DELETE CASCADE,
    linea INT NOT NULL,
    matricula VARCHAR(30),
    email VARCHAR(255),
    nombre VARCHAR(200),
    resultado VARCHAR(30) NOT NULL,
    detalle VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_item_resultado CHECK (resultado IN
        ('NUEVO','EXISTE_ACTIVA','EXISTE_INVITADA','COLISION_EMAIL','COLISION_MATRICULA','ERROR_FORMATO'))
);

CREATE INDEX idx_lote_item_lote ON lote_importacion_item(lote_id);

ALTER TABLE usuario ADD COLUMN lote_importacion_id BIGINT REFERENCES lote_importacion(id);
