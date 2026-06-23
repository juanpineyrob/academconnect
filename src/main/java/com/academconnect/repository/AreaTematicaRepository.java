package com.academconnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.ThesaurusOrigen;

public interface AreaTematicaRepository extends JpaRepository<AreaTematica, Long> {

    List<AreaTematica> findByThesaurusOrigenAndActivoTrue(ThesaurusOrigen origen);

    List<AreaTematica> findByParentId(Long parentId);

    Optional<AreaTematica> findByCodigoExternoAndThesaurusOrigen(String codigo, ThesaurusOrigen origen);

    /** Búsqueda admin paginada (nombre/código). {@code patron} ya viene como %…% en minúsculas (o null). */
    @Query("SELECT a FROM AreaTematica a WHERE :patron IS NULL " +
           "OR LOWER(a.nombre) LIKE :patron " +
           "OR LOWER(COALESCE(a.codigoExterno, '')) LIKE :patron")
    Page<AreaTematica> buscarAdmin(@Param("patron") String patron, Pageable pageable);
}
