package com.academconnect.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.academconnect.AbstractJpaTest;

/** Verifica que las queries paginadas de rúbricas se ejecutan contra Postgres real (no solo mocks). */
class TemplateEvaluacionBuscarTests extends AbstractJpaTest {

    @Autowired
    private TemplateEvaluacionRepository repository;

    @Test
    void buscarMiasSeEjecuta() {
        var page = repository.buscarMias(1L, PageRequest.of(0, 12, Sort.by("nombre")));
        Assertions.assertNotNull(page);
    }

    @Test
    void buscarPublicasSeEjecuta() {
        var page = repository.buscarPublicas(1L, PageRequest.of(0, 12, Sort.by("nombre")));
        Assertions.assertNotNull(page);
    }
}
