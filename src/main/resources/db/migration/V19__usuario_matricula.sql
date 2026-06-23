-- Matrícula del usuario: identificador del proceso de solicitud de cuenta y para
-- localizar usuarios fácilmente (búsqueda/autocomplete). El login sigue siendo por email.
-- Nullable + UNIQUE: no rompe inserciones legacy (varios NULL permitidos en Postgres);
-- las cuentas creadas por administración la exigen a nivel de aplicación.
ALTER TABLE usuario ADD COLUMN matricula VARCHAR(30);

-- Mock para usuarios existentes: matrícula numérica de 8 dígitos derivada del id.
UPDATE usuario SET matricula = LPAD(id::text, 8, '0') WHERE matricula IS NULL;

ALTER TABLE usuario ADD CONSTRAINT uq_usuario_matricula UNIQUE (matricula);
