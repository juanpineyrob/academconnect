-- Plantillas de evaluación (criterios dinámicos en JSONB).
CREATE TABLE template_evaluacion (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    descripcion TEXT,
    scope VARCHAR(40) NOT NULL,
    tipo_trabajo_aplicable VARCHAR(40),
    criterios JSONB NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_template_scope CHECK (
        scope IN ('INSTITUCIONAL','POR_TIPO_TRABAJO','POR_TRABAJO')
    ),
    CONSTRAINT chk_template_tipo CHECK (
        tipo_trabajo_aplicable IS NULL OR tipo_trabajo_aplicable IN
        ('TCC','TESIS','PAPER','MONOGRAFIA','PROYECTO_INVESTIGACION')
    )
);

CREATE INDEX idx_template_scope ON template_evaluacion(scope);
CREATE INDEX idx_template_tipo ON template_evaluacion(tipo_trabajo_aplicable);

-- Recomendaciones del algoritmo (output, separado de la asignación efectiva).
CREATE TABLE recomendacion_evaluador (
    id BIGSERIAL PRIMARY KEY,
    trabajo_id BIGINT NOT NULL REFERENCES trabajo(id) ON DELETE CASCADE,
    evaluador_candidato_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    score NUMERIC(6,4) NOT NULL,
    factores JSONB NOT NULL,
    generada_en TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_recomendacion_score CHECK (score >= 0 AND score <= 1)
);

CREATE INDEX idx_recomendacion_trabajo ON recomendacion_evaluador(trabajo_id);
CREATE INDEX idx_recomendacion_candidato ON recomendacion_evaluador(evaluador_candidato_id);

-- Conflicto de Interés: excluye candidatos del algoritmo.
CREATE TABLE conflicto_interes (
    id BIGSERIAL PRIMARY KEY,
    evaluador_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    trabajo_id BIGINT NOT NULL REFERENCES trabajo(id) ON DELETE CASCADE,
    motivo VARCHAR(40) NOT NULL,
    descripcion TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT uq_coi_evaluador_trabajo UNIQUE (evaluador_id, trabajo_id),
    CONSTRAINT chk_coi_motivo CHECK (
        motivo IN ('ORIENTADOR','COORIENTADOR','COAUTOR','MISMA_INSTITUCION','DECLARADO','OTRO')
    )
);

CREATE INDEX idx_coi_trabajo ON conflicto_interes(trabajo_id);

-- Asignación efectiva (versión específica del trabajo + snapshot inmutable del template).
CREATE TABLE asignacion (
    id BIGSERIAL PRIMARY KEY,
    trabajo_id BIGINT NOT NULL REFERENCES trabajo(id) ON DELETE RESTRICT,
    versionamiento_id BIGINT NOT NULL REFERENCES versionamiento(id) ON DELETE RESTRICT,
    evaluador_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    template_snapshot JSONB NOT NULL,
    asignada_en TIMESTAMP WITH TIME ZONE NOT NULL,
    vencimiento_en TIMESTAMP WITH TIME ZONE,
    estado VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT uq_asignacion_trabajo_evaluador_version UNIQUE (trabajo_id, evaluador_id, versionamiento_id),
    CONSTRAINT chk_asignacion_estado CHECK (
        estado IN ('ACTIVA','CANCELADA','COMPLETADA')
    )
);

CREATE INDEX idx_asignacion_trabajo ON asignacion(trabajo_id);
CREATE INDEX idx_asignacion_evaluador ON asignacion(evaluador_id);
CREATE INDEX idx_asignacion_estado ON asignacion(estado);

-- Evaluación: 1:1 con asignacion. Calificación final derivada de CalificacionCriterio.
CREATE TABLE evaluacion (
    id BIGSERIAL PRIMARY KEY,
    asignacion_id BIGINT NOT NULL REFERENCES asignacion(id) ON DELETE CASCADE,
    estado VARCHAR(20) NOT NULL,
    calificacion_final NUMERIC(6,2),
    comentario_general TEXT,
    completada_en TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT uq_evaluacion_asignacion UNIQUE (asignacion_id),
    CONSTRAINT chk_evaluacion_estado CHECK (
        estado IN ('PENDIENTE','COMPLETADA','EXPIRADA')
    )
);

CREATE INDEX idx_evaluacion_estado ON evaluacion(estado);

-- Calificación por criterio (insumo de la calificacion_final).
CREATE TABLE calificacion_criterio (
    id BIGSERIAL PRIMARY KEY,
    evaluacion_id BIGINT NOT NULL REFERENCES evaluacion(id) ON DELETE CASCADE,
    criterio_codigo VARCHAR(100) NOT NULL,
    puntaje NUMERIC(6,2) NOT NULL,
    comentario TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT uq_calificacion_criterio UNIQUE (evaluacion_id, criterio_codigo)
);

CREATE INDEX idx_calificacion_criterio_evaluacion ON calificacion_criterio(evaluacion_id);
