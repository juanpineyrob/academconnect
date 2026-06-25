package com.academconnect.service;

import com.academconnect.domain.InstanciaEvaluacionConfig;
import com.academconnect.domain.ModoEvaluacion;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.TipoTrabajoConfig;
import com.academconnect.dto.InstanciaEvaluacionConfigInput;
import com.academconnect.dto.TipoTrabajoConfigRequest;
import com.academconnect.dto.TipoTrabajoConfigResponse;
import com.academconnect.repository.InstanciaEvaluacionConfigRepository;
import com.academconnect.repository.TipoTrabajoConfigRepository;
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

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TipoTrabajoConfigServiceTests {

    @InjectMocks private TipoTrabajoConfigService service;
    @Mock private TipoTrabajoConfigRepository repository;
    @Mock private InstanciaEvaluacionConfigRepository instanciaRepository;

    @BeforeEach
    void setup() {
        Mockito.when(repository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        Mockito.when(instanciaRepository.findByTipoOrderByOrden(Mockito.any())).thenReturn(List.of());
    }

    private TipoTrabajoConfigRequest req(List<InstanciaEvaluacionConfigInput> instancias) {
        return new TipoTrabajoConfigRequest(ModoEvaluacion.SINCRONO, 3, true, instancias);
    }

    @Test
    void actualizar_reemplazaInstanciasConOrdenContiguo() {
        var req = req(List.of(
                new InstanciaEvaluacionConfigInput("TCC1", 2, 1),
                new InstanciaEvaluacionConfigInput("TCC2", 2, 1)));

        service.actualizar(TipoTrabajo.TCC, req);

        Mockito.verify(instanciaRepository).deleteByTipo(TipoTrabajo.TCC);
        ArgumentCaptor<List<InstanciaEvaluacionConfig>> cap = ArgumentCaptor.forClass(List.class);
        Mockito.verify(instanciaRepository).saveAll(cap.capture());
        List<InstanciaEvaluacionConfig> guardadas = cap.getValue();
        Assertions.assertEquals(2, guardadas.size());
        Assertions.assertEquals(0, guardadas.get(0).getOrden());
        Assertions.assertEquals("TCC1", guardadas.get(0).getNombre());
        Assertions.assertEquals(1, guardadas.get(1).getOrden());
        Assertions.assertEquals(TipoTrabajo.TCC, guardadas.get(0).getTipo());
    }

    @Test
    void actualizar_conInstanciasNullNoGuardaNinguna() {
        service.actualizar(TipoTrabajo.TESIS, req(null));
        Mockito.verify(instanciaRepository).deleteByTipo(TipoTrabajo.TESIS);
        Mockito.verify(instanciaRepository).saveAll(List.of());
    }

    @Test
    void actualizar_preservaEvaluadoresDefault() {
        var resp = service.actualizar(TipoTrabajo.TCC, req(List.of()));
        Assertions.assertEquals(3, resp.evaluadoresDefault());
        Assertions.assertEquals(ModoEvaluacion.SINCRONO, resp.modoEvaluacion());
    }

    @Test
    void buscarPorTipo_incluyeInstanciasOrdenadas() {
        var cfg = new TipoTrabajoConfig();
        cfg.setTipo(TipoTrabajo.TCC);
        cfg.setModoEvaluacion(ModoEvaluacion.SINCRONO);
        cfg.setEvaluadoresDefault(2);
        Mockito.when(repository.findById(TipoTrabajo.TCC)).thenReturn(Optional.of(cfg));
        var i0 = new InstanciaEvaluacionConfig();
        i0.setTipo(TipoTrabajo.TCC); i0.setOrden(0); i0.setNombre("TCC1"); i0.setEvaluadoresRequeridos(2);
        Mockito.when(instanciaRepository.findByTipoOrderByOrden(TipoTrabajo.TCC)).thenReturn(List.of(i0));

        TipoTrabajoConfigResponse resp = service.buscarPorTipo(TipoTrabajo.TCC);

        Assertions.assertEquals(1, resp.instancias().size());
        Assertions.assertEquals("TCC1", resp.instancias().get(0).nombre());
        Assertions.assertEquals(0, resp.instancias().get(0).orden());
    }

    @Test
    void actualizar_persisteSecuencialYMaxIntentos() {
        var req = new TipoTrabajoConfigRequest(ModoEvaluacion.SINCRONO, 3, false,
                List.of(new InstanciaEvaluacionConfigInput("TCC1", 2, 3)));
        var resp = service.actualizar(TipoTrabajo.TCC, req);
        Assertions.assertFalse(resp.secuencial());
        ArgumentCaptor<List<InstanciaEvaluacionConfig>> cap = ArgumentCaptor.forClass(List.class);
        Mockito.verify(instanciaRepository).saveAll(cap.capture());
        Assertions.assertEquals(3, cap.getValue().get(0).getMaxIntentos());
    }
}
