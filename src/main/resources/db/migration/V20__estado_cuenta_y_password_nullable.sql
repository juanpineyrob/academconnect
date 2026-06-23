-- Ciclo de vida de la credencial, ortogonal a `activo` (suspensión admin).
-- INVITADA: cuenta creada, sin contraseña, espera activación por token al email.
-- ACTIVA: el usuario probó control del email y fijó contraseña.
ALTER TABLE usuario ADD COLUMN estado_cuenta VARCHAR(20) NOT NULL DEFAULT 'ACTIVA';

-- Backfill: los usuarios existentes ya tienen contraseña -> ACTIVA (login intacto).
UPDATE usuario SET estado_cuenta = 'ACTIVA';

-- Las cuentas INVITADA no tienen contraseña (nunca seteada por admin ni importada).
ALTER TABLE usuario ALTER COLUMN password DROP NOT NULL;

ALTER TABLE usuario ADD CONSTRAINT chk_usuario_estado_cuenta
    CHECK (estado_cuenta IN ('INVITADA', 'ACTIVA'));

-- Invariante: contraseña presente si y solo si la cuenta está ACTIVA.
ALTER TABLE usuario ADD CONSTRAINT chk_usuario_password_estado
    CHECK ((estado_cuenta = 'ACTIVA' AND password IS NOT NULL)
        OR (estado_cuenta = 'INVITADA' AND password IS NULL));

CREATE INDEX idx_usuario_estado_cuenta ON usuario(estado_cuenta);
