-- Rúbricas generales con visibilidad y autor (Spec 1)
ALTER TABLE template_evaluacion ADD COLUMN visibilidad VARCHAR(20) NOT NULL DEFAULT 'PRIVADO';
ALTER TABLE template_evaluacion ADD COLUMN autor_id BIGINT REFERENCES usuario(id);
ALTER TABLE template_evaluacion ALTER COLUMN scope DROP NOT NULL;
ALTER TABLE template_evaluacion ADD CONSTRAINT chk_template_visibilidad
  CHECK (visibilidad IN ('PUBLICO','PRIVADO'));
