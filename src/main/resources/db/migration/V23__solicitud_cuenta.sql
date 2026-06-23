-- Self-request: una persona pide una cuenta; el admin la aprueba/rechaza.
-- Anti-enumeración en el endpoint público; la decisión la toma el admin.
CREATE TABLE solicitud_cuenta (
    id BIGSERIAL PRIMARY KEY,
    matricula VARCHAR(30) NOT NULL,
    email VARCHAR(255) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    motivo_rechazo VARCHAR(500),
    decidido_por_id BIGINT REFERENCES usuario(id),
    decidido_en TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_solicitud_estado CHECK (estado IN ('PENDIENTE','APROBADA','RECHAZADA'))
);

CREATE INDEX idx_solicitud_cuenta_estado ON solicitud_cuenta(estado);
