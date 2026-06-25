package com.academconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.TestcontainersConfiguration;
import com.academconnect.domain.EstadoInstanciaEvaluacion;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.InstanciaEvaluacion;
import com.academconnect.domain.InstanciaEvaluacionConfig;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.TipoTrabajoConfig;
import com.academconnect.domain.Trabajo;
import com.academconnect.repository.EvaluacionRepository;
import com.academconnect.repository.EstudianteRepository;
import com.academconnect.repository.InstanciaEvaluacionConfigRepository;
import com.academconnect.repository.InstanciaEvaluacionRepository;
import com.academconnect.repository.TipoTrabajoConfigRepository;
import com.academconnect.repository.TrabajoRepository;

/**
 * Integration test for the multi-instance evaluation pipeline (4b).
 *
 * <p>Runs against a real Postgres DB (Testcontainers + Flyway) to prove that:
 * <ol>
 *   <li>(C2 fix) {@code alReprobar} does NOT violate {@code uq_instancia_eval_abierta} when
 *       retrying — the {@code repository.flush()} before the INSERT ensures the old REPROBADA row
 *       is written before the new PENDIENTE row is inserted.</li>
 *   <li>The happy-path approve chain works end-to-end on the real schema.</li>
 *   <li>{@link EvaluacionRepository#promedioPorInstancia} JPQL compiles and runs against the
 *       real schema without error.</li>
 * </ol>
 *
 * <p>Each test method is {@link Transactional} so all writes are rolled back automatically,
 * keeping tests isolated.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class InstanciaEvaluacionPipelineIT {

    @Autowired InstanciaEvaluacionService pipelineService;
    @Autowired InstanciaEvaluacionRepository instanciaRepo;
    @Autowired InstanciaEvaluacionConfigRepository configRepo;
    @Autowired TipoTrabajoConfigRepository tipoConfigRepo;
    @Autowired TrabajoRepository trabajoRepo;
    @Autowired EstudianteRepository estudianteRepo;
    @Autowired EvaluacionRepository evaluacionRepo;

    /** A fresh trabajo of type TCC persisted for each test (rolled back after). */
    private Trabajo trabajo;
    /** TCC instancia config at orden=0 with maxIntentos=2 (for retry testing). */
    private InstanciaEvaluacionConfig c0;
    /** TCC instancia config at orden=1 with maxIntentos=1. */
    private InstanciaEvaluacionConfig c1;

    @BeforeEach
    void setup() {
        // Reuse the TCC instancia_evaluacion_config rows seeded by V28 (orden 0 and 1).
        // Update c0's maxIntentos to 2 so we can exercise the retry path.
        List<InstanciaEvaluacionConfig> tccConfigs = configRepo.findByTipoOrderByOrden(TipoTrabajo.TCC);
        assertThat(tccConfigs).as("V28 should seed 2 TCC instancia configs").hasSizeGreaterThanOrEqualTo(2);

        c0 = tccConfigs.get(0);
        c0.setMaxIntentos(2);
        configRepo.save(c0);

        c1 = tccConfigs.get(1);
        // c1.maxIntentos remains 1 (default from V29) — no retries allowed.

        // Also ensure TCC tipo_trabajo_config exists (seeded by V9) and is secuencial.
        TipoTrabajoConfig tipoCfg = tipoConfigRepo.findById(TipoTrabajo.TCC).orElseGet(() -> {
            var cfg = new TipoTrabajoConfig();
            cfg.setTipo(TipoTrabajo.TCC);
            cfg.setModoEvaluacion(com.academconnect.domain.ModoEvaluacion.SINCRONO);
            cfg.setEvaluadoresDefault(2);
            cfg.setSecuencial(true);
            return tipoConfigRepo.save(cfg);
        });
        tipoCfg.setSecuencial(true);
        tipoConfigRepo.save(tipoCfg);

        // Build a minimal fixture: one Estudiante + one TCC Trabajo in EN_DESARROLLO.
        var estudiante = new Estudiante();
        estudiante.setEmail("student-it-" + System.nanoTime() + "@test.local");
        estudiante.setNombre("IT Estudiante");
        estudiante.setPassword("encoded");
        estudiante.setActivo(true);
        estudiante = estudianteRepo.save(estudiante);

        trabajo = new Trabajo();
        trabajo.setTitulo("TCC IT Pipeline");
        trabajo.setTipo(TipoTrabajo.TCC);
        trabajo.setEstado(EstadoTrabajo.EN_DESARROLLO);
        trabajo.setEstudiante(estudiante);
        // DB constraint chk_trabajo_keywords_cardinality requires 3–8 keywords.
        trabajo.setKeywords(List.of("evaluacion", "pipeline", "integracion"));
        trabajo = trabajoRepo.save(trabajo);

        // Flush so the foreign keys resolve before service calls.
        trabajoRepo.flush();
    }

    /**
     * Scenario 1 (C2 fix): retry path does NOT throw a unique-constraint violation.
     *
     * <p>RED (before fix): {@code alReprobar} with an available retry would trigger
     * {@code uq_instancia_eval_abierta} because Hibernate flushed the INSERT of the new
     * PENDIENTE row before the UPDATE that marks the old row as REPROBADA.
     *
     * <p>GREEN (after fix): {@code repository.flush()} in {@code alReprobar} forces the
     * UPDATE to reach the DB before the INSERT, so only one open row exists at all times.
     */
    @Test
    void alReprobar_reintento_noViolaConstraintUnico() {
        // Materialize c0, intento 1.
        var instancia1 = pipelineService.materializarInicial(trabajo).orElseThrow();
        assertThat(instancia1.getOrden()).isEqualTo(0);
        assertThat(instancia1.getIntento()).isEqualTo(1);

        pipelineService.marcarEnCurso(instancia1);

        // Reprobar intento 1 — should produce a retry (intento 2) WITHOUT constraint violation.
        assertThatNoException()
                .as("alReprobar intento 1 should NOT violate uq_instancia_eval_abierta")
                .isThrownBy(() -> pipelineService.alReprobar(instancia1, new BigDecimal("3.00")));

        // Verify old instancia is REPROBADA and a new PENDIENTE instancia exists with intento=2.
        assertThat(instancia1.getEstado()).isEqualTo(EstadoInstanciaEvaluacion.REPROBADA);

        List<InstanciaEvaluacion> all = instanciaRepo.findByTrabajoIdOrderByOrdenAscIntentoAsc(trabajo.getId());
        assertThat(all).hasSize(2);
        var intento2 = all.stream()
                .filter(ie -> ie.getIntento() == 2)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No intento=2 instancia found"));
        assertThat(intento2.getEstado()).isEqualTo(EstadoInstanciaEvaluacion.PENDIENTE);
        assertThat(intento2.getInstanciaConfig().getId()).isEqualTo(c0.getId());

        // Exhaust retries: reprobar intento 2 (maxIntentos=2, so no further retry).
        pipelineService.marcarEnCurso(intento2);
        assertThatNoException()
                .as("alReprobar intento 2 (maxIntentos reached) should reject trabajo")
                .isThrownBy(() -> pipelineService.alReprobar(intento2, new BigDecimal("2.00")));

        var reloadedTrabajo = trabajoRepo.findById(trabajo.getId()).orElseThrow();
        assertThat(reloadedTrabajo.getEstado())
                .as("Trabajo should be RECHAZADO after exhausting retries")
                .isEqualTo(EstadoTrabajo.RECHAZADO);
    }

    /**
     * Scenario 2: approve chain — c0 approve → c1 materializes → c1 approve → APROBADO.
     */
    @Test
    void alAprobar_cadenaSecuencial_apruebaTrabajo() {
        // Materialize c0.
        var instanciaC0 = pipelineService.materializarInicial(trabajo).orElseThrow();
        assertThat(instanciaC0.getOrden()).isEqualTo(0);

        pipelineService.marcarEnCurso(instanciaC0);

        // Approve c0 → c1 should be materialized.
        assertThatNoException()
                .isThrownBy(() -> pipelineService.alAprobar(instanciaC0, new BigDecimal("8.00")));

        assertThat(instanciaC0.getEstado()).isEqualTo(EstadoInstanciaEvaluacion.APROBADA);

        List<InstanciaEvaluacion> afterC0 = instanciaRepo.findByTrabajoIdOrderByOrdenAscIntentoAsc(trabajo.getId());
        assertThat(afterC0).hasSize(2);
        var instanciaC1 = afterC0.stream()
                .filter(ie -> ie.getOrden() == 1 && ie.getEstado() == EstadoInstanciaEvaluacion.PENDIENTE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("c1 instancia not materialized after approving c0"));

        // Trabajo should still be EN_DESARROLLO (not yet finished).
        assertThat(trabajoRepo.findById(trabajo.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoTrabajo.EN_DESARROLLO);

        pipelineService.marcarEnCurso(instanciaC1);

        // Approve c1 (last) → trabajo should be APROBADO.
        assertThatNoException()
                .isThrownBy(() -> pipelineService.alAprobar(instanciaC1, new BigDecimal("9.00")));

        assertThat(instanciaC1.getEstado()).isEqualTo(EstadoInstanciaEvaluacion.APROBADA);
        assertThat(trabajoRepo.findById(trabajo.getId()).orElseThrow().getEstado())
                .as("Trabajo should be APROBADO after all instances approved")
                .isEqualTo(EstadoTrabajo.APROBADO);
    }

    /**
     * Exercises {@link EvaluacionRepository#promedioPorInstancia} JPQL against the real schema.
     * For an instance with no completed evaluations the result should be null (not throw).
     */
    @Test
    void promedioPorInstancia_sinEvaluaciones_retornaNull() {
        var instancia = pipelineService.materializarInicial(trabajo).orElseThrow();

        BigDecimal result = evaluacionRepo.promedioPorInstancia(instancia.getId());

        assertThat(result)
                .as("promedioPorInstancia should return null when no completed evaluations exist")
                .isNull();
    }
}
