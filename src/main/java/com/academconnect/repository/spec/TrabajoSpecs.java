package com.academconnect.repository.spec;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.Trabajo;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

/**
 * G12+G13 — predicados componibles para {@code GET /api/trabajos/buscar}.
 * Cada método devuelve null cuando no hay filtro aplicable; el caller filtra los nulls.
 *
 * <p>La búsqueda full-text no se modela como Specification (requeriría registrar una función
 * Hibernate para {@code tsvector @@ tsquery}). En su lugar, el servicio resuelve primero los
 * IDs vía query nativa contra {@code search_vector} y luego compone esos IDs con {@link #idEnConjunto}.
 */
public final class TrabajoSpecs {

    private TrabajoSpecs() {}

    public static Specification<Trabajo> idEnConjunto(List<Long> ids) {
        if (ids == null) return null;
        if (ids.isEmpty()) return (root, cq, cb) -> cb.disjunction(); // siempre falso
        return (root, cq, cb) -> root.get("id").in(ids);
    }

    /** OR sobre áreas: el trabajo tiene al menos una de las áreas indicadas. */
    public static Specification<Trabajo> tieneAlgunArea(List<Long> areaIds) {
        if (areaIds == null || areaIds.isEmpty()) return null;
        return (root, cq, cb) -> {
            if (Long.class != cq.getResultType()) cq.distinct(true);
            Join<Trabajo, AreaTematica> areas = root.join("areas", JoinType.INNER);
            return areas.get("id").in(areaIds);
        };
    }

    /** OR sobre años de creación (EXTRACT YEAR FROM createdAt). */
    public static Specification<Trabajo> creadoEnAnio(List<Integer> anios) {
        if (anios == null || anios.isEmpty()) return null;
        return (root, cq, cb) -> {
            Expression<Integer> anio = cb.function(
                    "date_part", Integer.class, cb.literal("year"), root.get("createdAt"));
            return anio.in(anios);
        };
    }

    public static Specification<Trabajo> tipoIgual(TipoTrabajo tipo) {
        if (tipo == null) return null;
        return (root, cq, cb) -> cb.equal(root.get("tipo"), tipo);
    }

    public static Specification<Trabajo> estadoIgual(EstadoTrabajo estado) {
        if (estado == null) return null;
        return (root, cq, cb) -> cb.equal(root.get("estado"), estado);
    }

    public static Specification<Trabajo> estadoEn(List<EstadoTrabajo> estados) {
        if (estados == null || estados.isEmpty()) return null;
        return (root, cq, cb) -> root.get("estado").in(estados);
    }

    /** Excluye trabajos ocultados por moderación de administrador (repositorio público). */
    public static Specification<Trabajo> noOculto() {
        return (root, cq, cb) -> cb.isFalse(root.get("oculto"));
    }

    public static Specification<Trabajo> orientadorIgual(Long orientadorId) {
        if (orientadorId == null) return null;
        return (root, cq, cb) -> cb.equal(root.get("orientador").get("id"), orientadorId);
    }

    /** Faltante señalado en la tabla: listar trabajos de un autor (estudiante) en perfil público. */
    public static Specification<Trabajo> estudianteIgual(Long estudianteId) {
        if (estudianteId == null) return null;
        return (root, cq, cb) -> cb.equal(root.get("estudiante").get("id"), estudianteId);
    }
}
