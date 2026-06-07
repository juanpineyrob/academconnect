package com.academconnect.job;

import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.Trabajo;
import com.academconnect.event.ActividadEvent;
import com.academconnect.repository.SolicitudVinculacionRepository;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.service.TrabajoService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrabajoExpiracionJobTests {

    @InjectMocks private TrabajoExpiracionJob job;
    @Mock private TrabajoRepository trabajoRepository;
    @Mock private TrabajoService trabajoService;
    @Mock private SolicitudVinculacionRepository solicitudRepository;
    @Mock private ApplicationEventPublisher events;

    private Trabajo vencido;

    @BeforeEach
    void setup() {
        vencido = new Trabajo();
        vencido.setId(100L);
        vencido.setTitulo("X");
        vencido.setEstado(EstadoTrabajo.ABIERTO);
        vencido.setExpiraEn(Instant.now().minusSeconds(3600));
        Mockito.when(trabajoRepository.findByEstadoAndExpiraEnBefore(
                Mockito.eq(EstadoTrabajo.ABIERTO), Mockito.any()))
                .thenReturn(List.of(vencido));
        Mockito.when(trabajoRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void expirarVencidosTransicionaACanceladoYInvocaHelper() {
        job.expirarVencidos();
        Assertions.assertEquals(EstadoTrabajo.CANCELADO, vencido.getEstado());
        Mockito.verify(trabajoService).autoRechazarPendientes(vencido, "Trabajo expirado");
    }

    @Test
    void expirarVencidosPublicaEventoTrabajoExpirado() {
        job.expirarVencidos();
        ArgumentCaptor<ActividadEvent> c = ArgumentCaptor.forClass(ActividadEvent.class);
        Mockito.verify(events).publishEvent(c.capture());
        Assertions.assertEquals(TipoActividad.TRABAJO_EXPIRADO, c.getValue().tipo());
    }

    @Test
    void expirarVencidosSinVencidosNoHaceNada() {
        Mockito.when(trabajoRepository.findByEstadoAndExpiraEnBefore(
                Mockito.eq(EstadoTrabajo.ABIERTO), Mockito.any()))
                .thenReturn(List.of());
        job.expirarVencidos();
        Mockito.verify(trabajoRepository, Mockito.never()).save(Mockito.any());
        Mockito.verifyNoInteractions(trabajoService);
    }
}
