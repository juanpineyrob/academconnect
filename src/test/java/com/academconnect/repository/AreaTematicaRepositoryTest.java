package com.academconnect.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.academconnect.AbstractJpaTest;
import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.ThesaurusOrigen;

class AreaTematicaRepositoryTest extends AbstractJpaTest {

    @Autowired private AreaTematicaRepository repo;

    @Test
    void seed_CNPq_cargado_con_jerarquia() {
        List<AreaTematica> cnpq = repo.findByThesaurusOrigenAndActivoTrue(ThesaurusOrigen.CNPQ);
        assertThat(cnpq).isNotEmpty();

        AreaTematica computacion = repo.findByCodigoExternoAndThesaurusOrigen("1.03.00.00-7", ThesaurusOrigen.CNPQ)
                .orElseThrow();
        assertThat(computacion.getNombre()).isEqualTo("Ciência da Computação");
        assertThat(computacion.getParent()).isNotNull();
        assertThat(computacion.getParent().getCodigoExterno()).isEqualTo("1.00.00.00-3");

        List<AreaTematica> subAreas = repo.findByParentId(computacion.getId());
        assertThat(subAreas).extracting(AreaTematica::getCodigoExterno)
                .contains("1.03.01.00-3", "1.03.02.00-0", "1.03.03.00-6", "1.03.04.00-2");
    }
}
