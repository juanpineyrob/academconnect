-- Full-text search column (GENERATED STORED, actualizada automáticamente por PG).
ALTER TABLE trabajo
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        to_tsvector('spanish',
            coalesce(titulo, '') || ' ' || coalesce(descripcion, ''))
    ) STORED;

CREATE INDEX idx_trabajo_fts ON trabajo USING GIN(search_vector);
