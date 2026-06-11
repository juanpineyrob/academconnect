-- V15__limpiar_areas_explicitas_alumnos.sql

-- Las áreas temáticas del alumno ahora se derivan automáticamente de sus trabajos
-- APROBADO (ver TrabajoRepository.areasDerivadas y PerfilService.computarAreas).
-- El endpoint PUT /me/areas además bloquea ediciones manuales para ESTUDIANTE.
-- Esta migración purga las asignaciones explícitas que pudieran haber quedado en
-- usuario_area_tematica para usuarios de tipo ESTUDIANTE.

DELETE FROM usuario_area_tematica
WHERE usuario_id IN (
    SELECT id FROM usuario WHERE dtype = 'ESTUDIANTE'
);
