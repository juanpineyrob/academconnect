package com.academconnect.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.academconnect.AbstractJpaTest;

class AreaTematicaBuscarTests extends AbstractJpaTest {

    @Autowired
    private AreaTematicaRepository repository;

    @Test
    void buscarAdminSinTextoNoExplota() {
        var page = repository.buscarAdmin(null, PageRequest.of(0, 10, Sort.by("nombre")));
        Assertions.assertNotNull(page);
    }

    @Test
    void buscarAdminConTextoNoExplota() {
        var page = repository.buscarAdmin("ia", PageRequest.of(0, 10, Sort.by("nombre")));
        Assertions.assertNotNull(page);
    }
}
