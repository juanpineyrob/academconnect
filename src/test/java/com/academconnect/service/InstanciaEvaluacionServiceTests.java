package com.academconnect.service;

import com.academconnect.domain.*;
import com.academconnect.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InstanciaEvaluacionServiceTests {

    @InjectMocks private InstanciaEvaluacionService service;
    @Mock private InstanciaEvaluacionRepository repository;
    @Mock private InstanciaEvaluacionConfigRepository configRepository;
    @Mock private TrabajoRepository trabajoRepository;
    @Mock private TipoTrabajoConfigRepository tipoTrabajoConfigRepository;

    private Trabajo trabajo;
    private InstanciaEvaluacionConfig c0, c1;

    @BeforeEach
    void setup() {
        trabajo = new Trabajo();
        trabajo.setId(100L);
        trabajo.setTipo(TipoTrabajo.TCC);
        trabajo.setEstado(EstadoTrabajo.EN_DESARROLLO);

        c0 = new InstanciaEvaluacionConfig();
        c0.setId(1L); c0.setTipo(TipoTrabajo.TCC); c0.setOrden(0); c0.setNombre("TCC1");
        c0.setEvaluadoresRequeridos(2); c0.setMaxIntentos(2);
        c1 = new InstanciaEvaluacionConfig();
        c1.setId(2L); c1.setTipo(TipoTrabajo.TCC); c1.setOrden(1); c1.setNombre("TCC2");
        c1.setEvaluadoresRequeridos(2); c1.setMaxIntentos(1);

        Mockito.when(configRepository.findByTipoOrderByOrden(TipoTrabajo.TCC)).thenReturn(List.of(c0, c1));
        Mockito.when(repository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        Mockito.when(trabajoRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
    }

    private TipoTrabajoConfig tipoCfg(boolean secuencial) {
        var t = new TipoTrabajoConfig();
        t.setTipo(TipoTrabajo.TCC); t.setModoEvaluacion(ModoEvaluacion.SINCRONO);
        t.setEvaluadoresDefault(2); t.setSecuencial(secuencial);
        return t;
    }

    private InstanciaEvaluacion inst(InstanciaEvaluacionConfig c, int intento, EstadoInstanciaEvaluacion estado) {
        var ie = new InstanciaEvaluacion();
        ie.setTrabajo(trabajo); ie.setInstanciaConfig(c); ie.setOrden(c.getOrden());
        ie.setIntento(intento); ie.setEstado(estado);
        return ie;
    }

    @Test
    void materializarInicial_creaPrimeraInstancia() {
        Mockito.when(repository.findFirstByTrabajoIdAndEstadoNotInOrderByOrdenAsc(
                Mockito.eq(100L), Mockito.anyCollection())).thenReturn(Optional.empty());

        var res = service.materializarInicial(trabajo);

        Assertions.assertTrue(res.isPresent());
        ArgumentCaptor<InstanciaEvaluacion> cap = ArgumentCaptor.forClass(InstanciaEvaluacion.class);
        Mockito.verify(repository).save(cap.capture());
        Assertions.assertEquals(0, cap.getValue().getOrden());
        Assertions.assertEquals(1, cap.getValue().getIntento());
        Assertions.assertEquals(EstadoInstanciaEvaluacion.PENDIENTE, cap.getValue().getEstado());
    }

    @Test
    void materializarInicial_sinConfigNoHaceNada() {
        Mockito.when(configRepository.findByTipoOrderByOrden(TipoTrabajo.TCC)).thenReturn(List.of());
        Assertions.assertTrue(service.materializarInicial(trabajo).isEmpty());
        Mockito.verify(repository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void materializarInicial_idempotenteSiYaExisteActiva() {
        Mockito.when(repository.findFirstByTrabajoIdAndEstadoNotInOrderByOrdenAsc(
                Mockito.eq(100L), Mockito.anyCollection()))
                .thenReturn(Optional.of(inst(c0, 1, EstadoInstanciaEvaluacion.PENDIENTE)));
        service.materializarInicial(trabajo);
        Mockito.verify(repository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void alAprobar_materializaSiguienteSiSecuencial() {
        Mockito.when(trabajoRepository.findById(100L)).thenReturn(Optional.of(trabajo));
        Mockito.when(tipoTrabajoConfigRepository.findById(TipoTrabajo.TCC)).thenReturn(Optional.of(tipoCfg(true)));
        var ie0 = inst(c0, 1, EstadoInstanciaEvaluacion.EN_CURSO);

        service.alAprobar(ie0, new BigDecimal("8.00"));

        Assertions.assertEquals(EstadoInstanciaEvaluacion.APROBADA, ie0.getEstado());
        // materializa c1 (orden 1)
        ArgumentCaptor<InstanciaEvaluacion> cap = ArgumentCaptor.forClass(InstanciaEvaluacion.class);
        Mockito.verify(repository, Mockito.atLeast(1)).save(cap.capture());
        Assertions.assertTrue(cap.getAllValues().stream().anyMatch(x -> x.getOrden() == 1 && x.getIntento() == 1));
        Assertions.assertNotEquals(EstadoTrabajo.APROBADO, trabajo.getEstado()); // aún no
    }

    @Test
    void alAprobar_ultimaInstancia_apruebaTrabajo() {
        Mockito.when(trabajoRepository.findById(100L)).thenReturn(Optional.of(trabajo));
        Mockito.when(tipoTrabajoConfigRepository.findById(TipoTrabajo.TCC)).thenReturn(Optional.of(tipoCfg(true)));
        var ie1 = inst(c1, 1, EstadoInstanciaEvaluacion.EN_CURSO); // orden 1 = última

        service.alAprobar(ie1, new BigDecimal("9.00"));

        Assertions.assertEquals(EstadoTrabajo.APROBADO, trabajo.getEstado());
    }

    @Test
    void alReprobar_reintentaSiHayCupo() {
        Mockito.when(trabajoRepository.findById(100L)).thenReturn(Optional.of(trabajo));
        var ie0 = inst(c0, 1, EstadoInstanciaEvaluacion.EN_CURSO); // c0 maxIntentos=2

        service.alReprobar(ie0, new BigDecimal("3.00"));

        Assertions.assertEquals(EstadoInstanciaEvaluacion.REPROBADA, ie0.getEstado());
        ArgumentCaptor<InstanciaEvaluacion> cap = ArgumentCaptor.forClass(InstanciaEvaluacion.class);
        Mockito.verify(repository, Mockito.atLeast(1)).save(cap.capture());
        Assertions.assertTrue(cap.getAllValues().stream()
                .anyMatch(x -> x.getInstanciaConfig() == c0 && x.getIntento() == 2));
        Assertions.assertNotEquals(EstadoTrabajo.RECHAZADO, trabajo.getEstado());
    }

    @Test
    void alReprobar_sinCupoRechazaTrabajo() {
        Mockito.when(trabajoRepository.findById(100L)).thenReturn(Optional.of(trabajo));
        var ie1 = inst(c1, 1, EstadoInstanciaEvaluacion.EN_CURSO); // c1 maxIntentos=1

        service.alReprobar(ie1, new BigDecimal("2.00"));

        Assertions.assertEquals(EstadoTrabajo.RECHAZADO, trabajo.getEstado());
    }
}
