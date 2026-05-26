-- F12 — cierre de gaps backend ↔ prototipo (G11, G14, G16, G17, G18).
-- G16 (tipo en criterios) no requiere DDL: el JSONB ya es flexible; la validación vive en el servicio.
-- G11 tampoco requiere DDL: la semántica "publicado = APROBADO" se aplica filtrando en endpoints públicos (F13).

-- ============================================================================
-- G17 — Umbral de aprobación por template + agregación denormalizada en trabajo
-- ============================================================================

ALTER TABLE template_evaluacion
    ADD COLUMN umbral_aprobacion NUMERIC(6,2) NOT NULL DEFAULT 6.00;

ALTER TABLE trabajo
    ADD COLUMN puntaje_agregado NUMERIC(6,2),
    ADD COLUMN evaluado_en      TIMESTAMP WITH TIME ZONE;

-- ============================================================================
-- G18 — Visibilidad del comentario por criterio (default privado)
-- ============================================================================

ALTER TABLE calificacion_criterio
    ADD COLUMN comentario_privado BOOLEAN NOT NULL DEFAULT TRUE;

-- ============================================================================
-- G14 — Keywords obligatorias en Trabajo (3 a 8) y reintegro al tsvector
-- ============================================================================

-- Default '{}' temporal para no romper filas existentes; el CHECK se aplica después del backfill.
ALTER TABLE trabajo
    ADD COLUMN keywords TEXT[] NOT NULL DEFAULT '{}';

-- Backfill: filas existentes (dev/tests) reciben placeholders para satisfacer el CHECK.
UPDATE trabajo
SET keywords = ARRAY['general', 'investigacion', 'academico']
WHERE cardinality(keywords) < 3;

ALTER TABLE trabajo
    ADD CONSTRAINT chk_trabajo_keywords_cardinality
    CHECK (cardinality(keywords) BETWEEN 3 AND 8);

-- Recrear el tsvector: la columna GENERATED de V7 no admite ampliar la expresión.
-- Se usa un trigger porque `array_to_string` combinado con `to_tsvector('spanish', ...)`
-- no satisface el requisito IMMUTABLE de las columnas GENERATED en PostgreSQL.
DROP INDEX IF EXISTS idx_trabajo_fts;
ALTER TABLE trabajo DROP COLUMN search_vector;

ALTER TABLE trabajo ADD COLUMN search_vector tsvector;

CREATE OR REPLACE FUNCTION trabajo_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector('spanish',
        coalesce(NEW.titulo, '') || ' ' ||
        coalesce(NEW.descripcion, '') || ' ' ||
        coalesce(array_to_string(NEW.keywords, ' '), ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trabajo_search_vector_trigger
    BEFORE INSERT OR UPDATE OF titulo, descripcion, keywords ON trabajo
    FOR EACH ROW EXECUTE FUNCTION trabajo_search_vector_update();

-- Poblar la columna para filas existentes (no hay INSERT/UPDATE que dispare el trigger sobre ellas).
UPDATE trabajo SET search_vector = to_tsvector('spanish',
    coalesce(titulo, '') || ' ' ||
    coalesce(descripcion, '') || ' ' ||
    coalesce(array_to_string(keywords, ' '), ''));

CREATE INDEX idx_trabajo_fts ON trabajo USING GIN(search_vector);
