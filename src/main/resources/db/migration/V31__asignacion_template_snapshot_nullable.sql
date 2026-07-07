-- La rúbrica ya no se congela al asignar: la asignación nace sin rúbrica y el
-- evaluador la elige la primera vez que entra a la evaluación. Hasta entonces el
-- snapshot es NULL.
ALTER TABLE asignacion ALTER COLUMN template_snapshot DROP NOT NULL;
