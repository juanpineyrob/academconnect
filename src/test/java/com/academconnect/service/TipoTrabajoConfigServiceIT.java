package com.academconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.academconnect.TestcontainersConfiguration;
import com.academconnect.domain.ModoEvaluacion;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.dto.InstanciaEvaluacionConfigInput;
import com.academconnect.dto.TipoTrabajoConfigRequest;

/**
 * Regression test for the UNIQUE(tipo, orden) constraint violation that
 * occurred when Hibernate flushed INSERTs before DELETEs during a re-PUT.
 * Requires a real DB (Testcontainers + Flyway) — mocks cannot reproduce it.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TipoTrabajoConfigServiceIT {

    @Autowired
    TipoTrabajoConfigService service;

    @Test
    void actualizar_rePutSobreFilasSembradasNoViolaConstraint() {
        // TCC is seeded by V28 with orden 0 and 1 — first call already exercises
        // delete+reinsert over existing rows (the constraint-violation scenario).
        var req1 = new TipoTrabajoConfigRequest(
                ModoEvaluacion.SINCRONO, 2,
                List.of(
                        new InstanciaEvaluacionConfigInput("TCC1", 2),
                        new InstanciaEvaluacionConfigInput("TCC2", 2)));

        assertThatNoException()
                .isThrownBy(() -> service.actualizar(TipoTrabajo.TCC, req1));

        // Second call — the explicit regression: a re-PUT after the first write
        // must not throw a UniqueConstraintViolationException.
        var req2 = new TipoTrabajoConfigRequest(
                ModoEvaluacion.SINCRONO, 3,
                List.of(
                        new InstanciaEvaluacionConfigInput("TCC1-v2", 2),
                        new InstanciaEvaluacionConfigInput("TCC2-v2", 3)));

        assertThatNoException()
                .isThrownBy(() -> service.actualizar(TipoTrabajo.TCC, req2));

        var result = service.buscarPorTipo(TipoTrabajo.TCC);
        assertThat(result.instancias()).hasSize(2);
        assertThat(result.instancias().get(0).orden()).isEqualTo(0);
        assertThat(result.instancias().get(1).orden()).isEqualTo(1);
        assertThat(result.instancias().get(0).nombre()).isEqualTo("TCC1-v2");
        assertThat(result.instancias().get(1).nombre()).isEqualTo("TCC2-v2");
        assertThat(result.evaluadoresDefault()).isEqualTo(3);
    }
}
