-- Primitiva única "probar control del email -> setear contraseña".
-- Sirve para ACTIVACION (cuenta INVITADA) y RESET (cuenta ACTIVA).
CREATE TABLE token_cuenta (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,           -- SHA-256 hex del token en claro
    proposito VARCHAR(20) NOT NULL,
    expira_en TIMESTAMP WITH TIME ZONE NOT NULL,
    usado_en TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT uq_token_cuenta_hash UNIQUE (token_hash),
    CONSTRAINT chk_token_proposito CHECK (proposito IN ('ACTIVACION', 'RESET'))
);

CREATE INDEX idx_token_cuenta_usuario ON token_cuenta(usuario_id, proposito);
