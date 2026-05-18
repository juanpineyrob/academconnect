-- Usuario base (herencia JPA JOINED).
CREATE TABLE usuario (
    id BIGSERIAL PRIMARY KEY,
    dtype VARCHAR(31) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    edad INTEGER,
    ubicacion VARCHAR(200),
    biografia TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT chk_usuario_edad CHECK (edad IS NULL OR edad > 0)
);

CREATE INDEX idx_usuario_dtype ON usuario(dtype);
CREATE INDEX idx_usuario_activo ON usuario(activo) WHERE activo = TRUE;

-- Subtablas
CREATE TABLE estudiante (
    id BIGINT PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE
);

CREATE TABLE profesor (
    id BIGINT PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE,
    titulacion VARCHAR(200),
    cargo VARCHAR(200)
);

CREATE TABLE externo (
    id BIGINT PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE,
    institucion VARCHAR(200) NOT NULL,
    titulo VARCHAR(200) NOT NULL
);

CREATE TABLE administrador (
    id BIGINT PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE
);

-- Áreas temáticas (thesaurus jerárquico).
CREATE TABLE area_tematica (
    id BIGSERIAL PRIMARY KEY,
    codigo_externo VARCHAR(50),
    nombre VARCHAR(200) NOT NULL,
    parent_id BIGINT REFERENCES area_tematica(id) ON DELETE RESTRICT,
    thesaurus_origen VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT uq_area_codigo_thesaurus UNIQUE (codigo_externo, thesaurus_origen),
    CONSTRAINT chk_area_thesaurus CHECK (thesaurus_origen IN ('CNPQ','ACM_CCS','INTERNO'))
);

CREATE INDEX idx_area_parent ON area_tematica(parent_id);
CREATE INDEX idx_area_thesaurus ON area_tematica(thesaurus_origen);

-- Áreas de experticia/interés del usuario (tabla de unión con atributo).
CREATE TABLE usuario_area_tematica (
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    area_id BIGINT NOT NULL REFERENCES area_tematica(id) ON DELETE RESTRICT,
    nivel_experticia VARCHAR(20),
    PRIMARY KEY (usuario_id, area_id),
    CONSTRAINT chk_uat_nivel CHECK (
        nivel_experticia IS NULL OR nivel_experticia IN ('BAJO','MEDIO','ALTO')
    )
);

CREATE INDEX idx_uat_area ON usuario_area_tematica(area_id);
