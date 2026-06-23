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

    /** Búsqueda administrativa paginada por texto (nombre/código). */
    @Query("SELECT a FROM AreaTematica a WHERE :q IS NULL " +
           "OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(COALESCE(a.codigoExterno, '')) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<AreaTematica> buscarAdmin(@Param("q") String q, Pageable pageable);
}
