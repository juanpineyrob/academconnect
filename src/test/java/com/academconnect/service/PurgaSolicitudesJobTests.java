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
import com.academconnect.domain.EstadoLote;
import com.academconnect.domain.EstadoSolicitudCuenta;
import com.academconnect.domain.LoteImportacion;
import com.academconnect.domain.LoteImportacionItem;
import com.academconnect.domain.ResultadoFila;
import com.academconnect.domain.SolicitudCuenta;
import com.academconnect.repository.LoteImportacionRepository;
import com.academconnect.repository.SolicitudCuentaRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PurgaSolicitudesJobTests {

    @Autowired
    private PurgaSolicitudesJob job;

    @Autowired
    private SolicitudCuentaRepository repo;

    @Autowired
    private LoteImportacionRepository loteRepository;

    private SolicitudCuenta seed(String email, EstadoSolicitudCuenta estado) {
        var s = new SolicitudCuenta();
        s.setMatricula("M-" + email);
        s.setEmail(email);
        s.setNombre("X");
        s.setEstado(estado);
        return repo.save(s);
    }

    private LoteImportacion seedLote(String nombreArchivo, EstadoLote estado) {
        var lote = new LoteImportacion();
        lote.setNombreArchivo(nombreArchivo);
        lote.setArchivoHash("hash-" + nombreArchivo);
        lote.setEstado(estado);
        // creado_por_id es nullable en V24 (BIGINT REFERENCES usuario(id) sin NOT NULL),
        // así que no hace falta sembrar un Administrador.
        return loteRepository.save(lote);
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
    void purgaSolicitudPendienteMayorA7Dias() {
        var pendiente = seed("pend@x.test", EstadoSolicitudCuenta.PENDIENTE);
        repo.flush();

        job.purgar(Instant.now().plus(8, ChronoUnit.DAYS));

        assertThat(repo.findById(pendiente.getId())).isEmpty();
    }

    @Test
    @Transactional
    void noPurgaSolicitudAprobada() {
        var aprobada = seed("aprob@x.test", EstadoSolicitudCuenta.APROBADA);
        repo.flush();

        job.purgar(Instant.now().plus(8, ChronoUnit.DAYS));

        assertThat(repo.findById(aprobada.getId())).isPresent();
    }

    @Test
    @Transactional
    void purgaLotePreviewMayorA24HorasYConservaConfirmado() {
        var preview = seedLote("preview.csv", EstadoLote.PREVIEW);
        var item = new LoteImportacionItem();
        item.setLote(preview);
        item.setLinea(1);
        item.setMatricula("M-1");
        item.setEmail("a@x.test");
        item.setNombre("A");
        item.setResultado(ResultadoFila.NUEVO);
        preview.getItems().add(item);
        loteRepository.save(preview);

        var confirmado = seedLote("confirmado.csv", EstadoLote.CONFIRMADO);
        loteRepository.flush();

        // now + 2 días supera el umbral de 24h de forma determinista.
        job.purgar(Instant.now().plus(2, ChronoUnit.DAYS));

        // El item cae por ON DELETE CASCADE (o el cascade JPA) al borrarse el lote.
        assertThat(loteRepository.findById(preview.getId())).isEmpty();
        assertThat(loteRepository.findById(confirmado.getId())).isPresent();
    }
}
