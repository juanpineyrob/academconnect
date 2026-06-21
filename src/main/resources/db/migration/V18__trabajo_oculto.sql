-- Moderación de administrador: ocultar un trabajo del repositorio público sin perder su estado
-- de workflow (reversible). Independiente de la eliminación física (override admin).
ALTER TABLE trabajo ADD COLUMN oculto BOOLEAN NOT NULL DEFAULT FALSE;
