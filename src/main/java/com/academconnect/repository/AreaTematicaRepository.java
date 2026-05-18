package com.academconnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.ThesaurusOrigen;

public interface AreaTematicaRepository extends JpaRepository<AreaTematica, Long> {

    List<AreaTematica> findByThesaurusOrigenAndActivoTrue(ThesaurusOrigen origen);

    List<AreaTematica> findByParentId(Long parentId);

    Optional<AreaTematica> findByCodigoExternoAndThesaurusOrigen(String codigo, ThesaurusOrigen origen);
}
