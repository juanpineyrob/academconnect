-- Outbox de mail: desacopla "encolar" de "enviar". Permite oleadas (drainer) y reintentos.
CREATE TABLE mail_pendiente (
    id BIGSERIAL PRIMARY KEY,
    destinatario VARCHAR(255) NOT NULL,
    asunto VARCHAR(300) NOT NULL,
    cuerpo_html TEXT NOT NULL,
    cuerpo_texto TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos INT NOT NULL DEFAULT 0,
    ultimo_error VARCHAR(500),
    enviado_en TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_mail_estado CHECK (estado IN ('PENDIENTE','ENVIADO','FALLIDO'))
);

CREATE INDEX idx_mail_pendiente_estado ON mail_pendiente(estado) WHERE estado = 'PENDIENTE';
