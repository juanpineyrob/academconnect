package com.academconnect.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.academconnect.AbstractJpaTest;
import com.academconnect.domain.TemplateEvaluacion;
import com.academconnect.domain.TemplateScope;
import com.academconnect.domain.TipoTrabajo;

class TemplateEvaluacionJsonbTest extends AbstractJpaTest {

    @Autowired private TemplateEvaluacionRepository repo;

    @Test
    void criterios_jsonb_roundtrip() {
        TemplateEvaluacion t = new TemplateEvaluacion();
        t.setNombre("Template TCC genérico");
        t.setScope(TemplateScope.POR_TIPO_TRABAJO);
        t.setTipoTrabajoAplicable(TipoTrabajo.TCC);
        t.setCriterios("""
                [
                  {"codigo":"metodologia","nombre":"Metodología","peso":0.4,"escalaMin":0,"escalaMax":10},
                  {"codigo":"originalidad","nombre":"Originalidad","peso":0.3,"escalaMin":0,"escalaMax":10},
                  {"codigo":"escritura","nombre":"Escritura","peso":0.3,"escalaMin":0,"escalaMax":10}
                ]
                """);

        TemplateEvaluacion saved = repo.saveAndFlush(t);
        repo.findById(saved.getId()).ifPresent(loaded -> {
            assertThat(loaded.getCriterios()).contains("\"metodologia\"");
            assertThat(loaded.getCriterios()).contains("\"originalidad\"");
        });
    }
}
