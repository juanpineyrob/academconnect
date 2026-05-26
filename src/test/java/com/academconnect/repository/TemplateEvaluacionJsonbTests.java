package com.academconnect.repository;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.academconnect.AbstractJpaTest;
import com.academconnect.domain.TemplateEvaluacion;
import com.academconnect.domain.TemplateScope;
import com.academconnect.domain.TipoTrabajo;

public class TemplateEvaluacionJsonbTests extends AbstractJpaTest {

    @Autowired
    private TemplateEvaluacionRepository repo;

    @Test
    void saveShouldRoundtripCriteriosJsonbWhenPersisted() {
        TemplateEvaluacion t = new TemplateEvaluacion();
        t.setNombre("Template TCC genérico");
        t.setScope(TemplateScope.POR_TIPO_TRABAJO);
        t.setTipoTrabajoAplicable(TipoTrabajo.TCC);
        t.setUmbralAprobacion(new BigDecimal("6.00"));
        t.setCriterios("""
                [
                  {"codigo":"metodologia","nombre":"Metodología","tipo":"ESCALA","peso":0.4,"escalaMin":0,"escalaMax":10},
                  {"codigo":"originalidad","nombre":"Originalidad","tipo":"ESCALA","peso":0.3,"escalaMin":0,"escalaMax":10},
                  {"codigo":"escritura","nombre":"Escritura","tipo":"ESCALA","peso":0.3,"escalaMin":0,"escalaMax":10}
                ]
                """);

        TemplateEvaluacion saved = repo.saveAndFlush(t);
        TemplateEvaluacion loaded = repo.findById(saved.getId()).orElseThrow();

        Assertions.assertTrue(loaded.getCriterios().contains("\"metodologia\""));
        Assertions.assertTrue(loaded.getCriterios().contains("\"originalidad\""));
        Assertions.assertEquals(0, loaded.getUmbralAprobacion().compareTo(new BigDecimal("6.00")));
    }
}
