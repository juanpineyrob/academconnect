package com.academconnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.TestcontainersConfiguration;
import com.academconnect.domain.EstadoSolicitudCuenta;
import com.academconnect.domain.SolicitudCuenta;
import com.academconnect.repository.SolicitudCuentaRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PurgaSolicitudesJobTests {

    @Autowired
    private PurgaSolicitudesJob job;

    @Autowired
    private SolicitudCuentaRepository repo;

    private SolicitudCuenta seed(String email, EstadoSolicitudCuenta estado) {
        var s = new SolicitudCuenta();
        s.setMatricula("M-" + email);
        s.setEmail(email);
        s.setNombre("X");
        s.setEstado(estado);
        return repo.save(s);
    }

    @Test
    @Transactional
    void purgaSolicitudRechazadaMayorA7Dias() {
        var rechazada = seed("rech@x.test", EstadoSolicitudCuenta.RECHAZADA);
        repo.flush();

        // "ahora" en el futuro (now + 8 días) hace que la fila recién guardada
        // cuente como más vieja que 7 días, de forma determinista.
        job.purgar(Instant.now().plus(8, ChronoUnit.DAYS));

        assertThat(repo.findById(rechazada.getId())).isEmpty();
    }

    @Test
    @Transactional
    void noPurgaSolicitudAprobada() {
        var aprobada = seed("aprob@x.test", EstadoSolicitudCuenta.APROBADA);
        repo.flush();

        job.purgar(Instant.now().plus(8, ChronoUnit.DAYS));

        assertThat(repo.findById(aprobada.getId())).isPresent();
    }
}
