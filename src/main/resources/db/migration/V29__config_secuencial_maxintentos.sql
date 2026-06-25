-- V29__config_secuencial_maxintentos.sql
ALTER TABLE tipo_trabajo_config
    ADD COLUMN secuencial BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE instancia_evaluacion_config
    ADD COLUMN max_intentos INTEGER NOT NULL DEFAULT 1;

ALTER TABLE instancia_evaluacion_config
    ADD CONSTRAINT chk_instancia_max_intentos CHECK (max_intentos >= 1);
