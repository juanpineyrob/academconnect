-- V26__template_por_defecto.sql

-- Rúbrica genérica por defecto: su snapshot se congela en la Asignacion creada
-- cuando un evaluador acepta una solicitud de evaluación.
ALTER TABLE template_evaluacion
    ADD COLUMN es_por_defecto BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO template_evaluacion (
    nombre, descripcion, visibilidad, criterios, activo, umbral_aprobacion,
    es_por_defecto, created_at, updated_at, created_by, updated_by
) VALUES (
    'Rúbrica genérica',
    'Rúbrica por defecto del sistema para evaluación de trabajos.',
    'PUBLICO',
    '[{"codigo":"metodologia","nombre":"Metodología","peso":0.4,"escalaMin":0,"escalaMax":10},{"codigo":"contenido","nombre":"Contenido","peso":0.4,"escalaMin":0,"escalaMax":10},{"codigo":"presentacion","nombre":"Presentación","peso":0.2,"escalaMin":0,"escalaMax":10}]'::jsonb,
    TRUE,
    6.00,
    TRUE,
    now(), now(), 'system', 'system'
);

-- A lo sumo un template por defecto.
CREATE UNIQUE INDEX uq_template_por_defecto
    ON template_evaluacion (es_por_defecto)
    WHERE es_por_defecto = TRUE;
