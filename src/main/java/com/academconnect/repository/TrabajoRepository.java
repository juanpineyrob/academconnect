package com.academconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.Trabajo;
import com.academconnect.dto.TrabajosPorEstadoDto;
import com.academconnect.dto.UsuarioAreaTematicaResponse;

public interface TrabajoRepository
        extends JpaRepository<Trabajo, Long>, JpaSpecificationExecutor<Trabajo> {

    List<Trabajo> findByEstudianteIdAndTipoAndEstadoIn(Long estudianteId, TipoTrabajo tipo, List<EstadoTrabajo> estados);

    List<Trabajo> findByOrientadorId(Long orientadorId);

    List<Trabajo> findByEstado(EstadoTrabajo estado);

    List<Trabajo> findByEstudianteId(Long estudianteId);

    long countByEstado(EstadoTrabajo estado);

    /** Para el conteo en perfil público — cuántos trabajos APROBADOS tiene este usuario. */
    long countByEstudianteIdAndEstado(Long estudianteId, EstadoTrabajo estado);

    long countByOrientadorIdAndEstado(Long orientadorId, EstadoTrabajo estado);

    @Query("SELECT new com.academconnect.dto.TrabajosPorEstadoDto(t.estado, COUNT(t)) FROM Trabajo t GROUP BY t.estado")
    List<TrabajosPorEstadoDto> contarPorEstado();

    @Query(value = "SELECT * FROM trabajo WHERE search_vector @@ plainto_tsquery('spanish', :q)",
           nativeQuery = true)
    List<Trabajo> buscarPorTexto(@org.springframework.data.repository.query.Param("q") String q);

    /** Devuelve los IDs que matchean el FTS sin cargar las entidades — para componer con Specifications. */
    @Query(value = "SELECT id FROM trabajo WHERE search_vector @@ plainto_tsquery('spanish', :q)",
           nativeQuery = true)
    List<Long> buscarIdsPorTexto(@org.springframework.data.repository.query.Param("q") String q);

    /** Cuenta áreas distintas vinculadas a trabajos en alguno de los estados dados. */
    @Query("SELECT COUNT(DISTINCT a.id) FROM Trabajo t JOIN t.areas a WHERE t.estado IN :estados")
    long countAreasDistintasConTrabajosEnEstados(
            @org.springframework.data.repository.query.Param("estados") List<EstadoTrabajo> estados);

    /** Cuenta trabajos APROBADOS en cuyo proceso evaluativo participó el evaluador dado. */
    @Query("SELECT COUNT(DISTINCT t) FROM Trabajo t " +
           "JOIN com.academconnect.domain.Asignacion a ON a.trabajo = t " +
           "WHERE t.estado = com.academconnect.domain.EstadoTrabajo.APROBADO " +
           "AND a.evaluador.id = :evaluadorId")
    long countAprobadosConEvaluadoresIncluyendo(
            @org.springframework.data.repository.query.Param("evaluadorId") Long evaluadorId);

    @Query("SELECT COUNT(DISTINCT t) FROM Trabajo t " +
           "JOIN com.academconnect.domain.Asignacion a ON a.trabajo = t " +
           "WHERE t.estado = com.academconnect.domain.EstadoTrabajo.RECHAZADO " +
           "AND a.evaluador.id = :evaluadorId")
    long countRechazadosConEvaluadoresIncluyendo(
            @org.springframework.data.repository.query.Param("evaluadorId") Long evaluadorId);

    List<Trabajo> findByEstadoAndExpiraEnBefore(EstadoTrabajo estado, java.time.Instant fecha);

    @Query("""
            SELECT a.id, a.nombre
            FROM Trabajo t
            JOIN t.areas a
            WHERE t.estado = com.academconnect.domain.EstadoTrabajo.APROBADO
              AND t.estudiante.id = :usuarioId
            GROUP BY a.id, a.nombre
            ORDER BY COUNT(t) DESC, a.nombre ASC
            """)
    List<Object[]> areasDerivadasRaw(
            @org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);

    /** Áreas temáticas del alumno, derivadas de sus trabajos APROBADO,
     *  ordenadas por cantidad de trabajos (desc) y luego por nombre.
     *  El campo nivelExperticia siempre es null (las áreas del alumno no tienen nivel). */
    default List<UsuarioAreaTematicaResponse> areasDerivadas(Long usuarioId) {
        return areasDerivadasRaw(usuarioId).stream()
                .map(row -> new UsuarioAreaTematicaResponse(
                        (Long) row[0],
                        (String) row[1],
                        null))
                .toList();
    }
}
