-- F14 — perfil del evaluador (G08, G22, G23, G24), sesiones (G20) y campos ampliados de perfil.

-- ============================================================================
-- G08 — Tope de asignaciones por usuario (default configurable)
-- foto_url para perfil (avatar)
-- ============================================================================

ALTER TABLE usuario
    ADD COLUMN tope_asignaciones INT NOT NULL DEFAULT 5,
    ADD COLUMN foto_url VARCHAR(500);

-- ============================================================================
-- G20 — Configuración por TipoTrabajo + sesión de evaluación (sync/hybrid)
-- ============================================================================

CREATE TABLE tipo_trabajo_config (
    tipo VARCHAR(50) PRIMARY KEY,
    modo_evaluacion VARCHAR(20) NOT NULL,
    evaluadores_default INT NOT NULL DEFAULT 3,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT chk_ttc_modo CHECK (modo_evaluacion IN ('SINCRONO','ASINCRONO','HIBRIDO')),
    CONSTRAINT chk_ttc_tipo CHECK (
        tipo IN ('TCC','TESIS','PAPER','MONOGRAFIA','PROYECTO_INVESTIGACION')
    )
);

INSERT INTO tipo_trabajo_config (tipo, modo_evaluacion) VALUES
    ('TCC', 'SINCRONO'),
    ('TESIS', 'HIBRIDO'),
    ('PAPER', 'ASINCRONO'),
    ('MONOGRAFIA', 'ASINCRONO'),
    ('PROYECTO_INVESTIGACION', 'ASINCRONO');

CREATE TABLE sesion_evaluacion (
    id BIGSERIAL PRIMARY KEY,
    trabajo_id BIGINT NOT NULL REFERENCES trabajo(id) ON DELETE CASCADE,
    fecha_programada TIMESTAMP WITH TIME ZONE NOT NULL,
    duracion_minutos INT NOT NULL,
    ubicacion VARCHAR(255),
    modalidad VARCHAR(20) NOT NULL,
    url_meet VARCHAR(500),
    estado VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_sesion_modalidad CHECK (modalidad IN ('PRESENCIAL','VIRTUAL')),
    CONSTRAINT chk_sesion_estado CHECK (estado IN ('PROGRAMADA','EN_CURSO','FINALIZADA','CANCELADA')),
    CONSTRAINT chk_sesion_duracion CHECK (duracion_minutos > 0)
);

CREATE INDEX idx_sesion_trabajo ON sesion_evaluacion(trabajo_id);
CREATE INDEX idx_sesion_fecha ON sesion_evaluacion(fecha_programada);
CREATE INDEX idx_sesion_estado ON sesion_evaluacion(estado);

ALTER TABLE asignacion ADD COLUMN sesion_id BIGINT REFERENCES sesion_evaluacion(id) ON DELETE SET NULL;
CREATE INDEX idx_asignacion_sesion ON asignacion(sesion_id);

-- ============================================================================
-- G23 — Disponibilidad del evaluador (horas por día)
-- ============================================================================

CREATE TABLE disponibilidad_evaluador (
    id BIGSERIAL PRIMARY KEY,
    evaluador_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    fecha DATE NOT NULL,
    horas_disponibles NUMERIC(4,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT uq_disponibilidad UNIQUE (evaluador_id, fecha),
    CONSTRAINT chk_disp_horas CHECK (horas_disponibles >= 0 AND horas_disponibles <= 24)
);

CREATE INDEX idx_disp_evaluador_fecha ON disponibilidad_evaluador(evaluador_id, fecha);

-- ============================================================================
-- G24 — Reconocimientos / badges
-- ============================================================================

CREATE TABLE reconocimiento (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    anio INT NOT NULL,
    otorgado_por BIGINT REFERENCES usuario(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_reconocimiento_anio CHECK (anio BETWEEN 1900 AND 2200)
);

CREATE INDEX idx_reconocimiento_usuario ON reconocimiento(usuario_id);
