package com.academconnect.repository;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.academconnect.AbstractJpaTest;
import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.ThesaurusOrigen;

public class AreaTematicaRepositoryTests extends AbstractJpaTest {

    @Autowired
    private AreaTematicaRepository repo;

    @Test
    void findByThesaurusOrigenShouldLoadSeedCnpqWithHierarchy() {
        List<AreaTematica> cnpq = repo.findByThesaurusOrigenAndActivoTrue(ThesaurusOrigen.CNPQ);
        Assertions.assertFalse(cnpq.isEmpty());

        AreaTematica computacion = repo
                .findByCodigoExternoAndThesaurusOrigen("1.03.00.00-7", ThesaurusOrigen.CNPQ)
                .orElseThrow();
        Assertions.assertEquals("Ciência da Computação", computacion.getNombre());
        Assertions.assertNotNull(computacion.getParent());
        Assertions.assertEquals("1.00.00.00-3", computacion.getParent().getCodigoExterno());

        List<String> subAreaCodigos = repo.findByParentId(computacion.getId()).stream()
                .map(AreaTematica::getCodigoExterno)
                .toList();
        Assertions.assertTrue(subAreaCodigos.contains("1.03.01.00-3"));
        Assertions.assertTrue(subAreaCodigos.contains("1.03.02.00-0"));
        Assertions.assertTrue(subAreaCodigos.contains("1.03.03.00-6"));
        Assertions.assertTrue(subAreaCodigos.contains("1.03.04.00-2"));
    }
}
