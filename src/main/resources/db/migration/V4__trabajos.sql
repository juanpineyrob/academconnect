CREATE TABLE trabajo (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(300) NOT NULL,
    descripcion TEXT,
    tipo VARCHAR(40) NOT NULL,
    estado VARCHAR(40) NOT NULL,
    orientador_id BIGINT NOT NULL REFERENCES profesor(id) ON DELETE RESTRICT,
    estudiante_id BIGINT REFERENCES estudiante(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_trabajo_tipo CHECK (
        tipo IN ('TCC','TESIS','PAPER','MONOGRAFIA','PROYECTO_INVESTIGACION')
    ),
    CONSTRAINT chk_trabajo_estado CHECK (
        estado IN ('BORRADOR','ABIERTO','EN_DESARROLLO','EN_EVALUACION','APROBADO','RECHAZADO','CANCELADO')
    )
);

CREATE INDEX idx_trabajo_orientador ON trabajo(orientador_id);
CREATE INDEX idx_trabajo_estudiante ON trabajo(estudiante_id);
CREATE INDEX idx_trabajo_estado ON trabajo(estado);
CREATE INDEX idx_trabajo_tipo ON trabajo(tipo);

-- Un estudiante no puede tener dos trabajos activos del mismo tipo simultáneamente.
-- Permite múltiples trabajos finalizados (APROBADO/RECHAZADO/CANCELADO) por reprobaciones.
CREATE UNIQUE INDEX uq_trabajo_estudiante_tipo_activo
    ON trabajo (estudiante_id, tipo)
    WHERE estudiante_id IS NOT NULL
      AND estado NOT IN ('APROBADO','RECHAZADO','CANCELADO');

-- Coorientadores: profesor o externo que coorientan, con rol descrito y vigencia.
CREATE TABLE coorientador (
    id BIGSERIAL PRIMARY KEY,
    trabajo_id BIGINT NOT NULL REFERENCES trabajo(id) ON DELETE CASCADE,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    rol_descrito VARCHAR(200),
    desde DATE,
    hasta DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT uq_coorientador_trabajo_usuario UNIQUE (trabajo_id, usuario_id),
    CONSTRAINT chk_coorientador_fechas CHECK (hasta IS NULL OR desde IS NULL OR hasta >= desde)
);

CREATE INDEX idx_coorientador_usuario ON coorientador(usuario_id);

-- Áreas temáticas del trabajo (insumo del algoritmo de asignación).
CREATE TABLE trabajo_area_tematica (
    trabajo_id BIGINT NOT NULL REFERENCES trabajo(id) ON DELETE CASCADE,
    area_id BIGINT NOT NULL REFERENCES area_tematica(id) ON DELETE RESTRICT,
    PRIMARY KEY (trabajo_id, area_id)
);

CREATE INDEX idx_tat_area ON trabajo_area_tematica(area_id);

-- Solicitudes de vinculación (estudiante → proyecto creado por profesor).
CREATE TABLE solicitud_vinculacion (
    id BIGSERIAL PRIMARY KEY,
    trabajo_id BIGINT NOT NULL REFERENCES trabajo(id) ON DELETE CASCADE,
    estudiante_id BIGINT NOT NULL REFERENCES estudiante(id) ON DELETE CASCADE,
    estado VARCHAR(20) NOT NULL,
    motivo TEXT,
    respuesta TEXT,
    resuelta_en TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_solicitud_estado CHECK (
        estado IN ('PENDIENTE','APROBADA','RECHAZADA','CANCELADA')
    )
);

CREATE INDEX idx_solicitud_trabajo ON solicitud_vinculacion(trabajo_id);
CREATE INDEX idx_solicitud_estudiante ON solicitud_vinculacion(estudiante_id);
CREATE INDEX idx_solicitud_estado ON solicitud_vinculacion(estado);

-- Solo una solicitud pendiente por (trabajo, estudiante) — el alumno no puede spammear.
CREATE UNIQUE INDEX uq_solicitud_trabajo_estudiante_pendiente
    ON solicitud_vinculacion (trabajo_id, estudiante_id)
    WHERE estado = 'PENDIENTE';
