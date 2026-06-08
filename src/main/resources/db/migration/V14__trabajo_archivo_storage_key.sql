-- V14__trabajo_archivo_storage_key.sql

-- Antes archivo_url guardaba el path público del estático /storage/trabajos/{id}.pdf.
-- Hoy el PDF se sirve por GET /api/trabajos/{id}/archivo con check de acceso, y la
-- columna sólo necesita la clave de almacenamiento (filename relativo a la raíz de
-- storage de trabajos). Renombramos y normalizamos los valores existentes.

ALTER TABLE trabajo RENAME COLUMN archivo_url TO archivo_storage_key;

UPDATE trabajo
SET archivo_storage_key = regexp_replace(archivo_storage_key, '^.*/', '')
WHERE archivo_storage_key IS NOT NULL;
