-- V13__trabajo_expira_en.sql

-- Camino 2.2 — el profesor publica un trabajo con TTL configurable.
ALTER TABLE trabajo ADD COLUMN expira_en TIMESTAMP WITH TIME ZONE;
CREATE INDEX ix_trabajo_estado_expira_en ON trabajo (estado, expira_en) WHERE expira_en IS NOT NULL;
