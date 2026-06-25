package com.academconnect.service;

import com.academconnect.domain.EstadoSolicitud;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.SolicitudVinculacion;
import com.academconnect.domain.Trabajo;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.exception.BusinessException;
import com.academconnect.factories.UsuarioFactory;
import com.academconnect.mapper.SolicitudVinculacionMapper;
import com.academconnect.repository.EstudianteRepository;
import com.academconnect.repository.SolicitudVinculacionRepository;
import com.academconnect.repository.TrabajoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudVinculacionServiceAceptarTests {

    @InjectMocks private SolicitudVinculacionService service;
    @Mock private SolicitudVinculacionRepository solicitudRepository;
    @Mock private TrabajoRepository trabajoRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private SolicitudVinculacionMapper mapper;
    @Mock private ApplicationEventPublisher events;
    @Mock private TrabajoService trabajoService;
    @Mock private InstanciaEvaluacionService instanciaEvaluacionService;

    private Estudiante estudiante;
    private Trabajo trabajo;
    private SolicitudVinculacion solicitud;

    @BeforeEach
    void setup() {
        estudiante = UsuarioFactory.createEstudiante(10L, "alumno@x.uy");
        trabajo = new Trabajo();
        trabajo.setId(100L);
        trabajo.setTitulo("TCC de prueba");
        trabajo.setTipo(TipoTrabajo.TCC);
        trabajo.setEstado(EstadoTrabajo.ABIERTO);

        solicitud = new SolicitudVinculacion();
        solicitud.setId(500L);
        solicitud.setTrabajo(trabajo);
        solicitud.setEstudiante(estudiante);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);

        when(solicitudRepository.findById(500L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(trabajoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(null);
    }

    @Test
    void aceptar_mueveTrabajoAEnDesarrolloYMaterializaInstancia() {
        service.aceptar(500L, null);

        assertEquals(EstadoTrabajo.EN_DESARROLLO, trabajo.getEstado());
        assertEquals(estudiante, trabajo.getEstudiante());
        // C1 fix: materializarInicial debe invocarse tras el save del trabajo
        verify(instanciaEvaluacionService).materializarInicial(trabajo);
    }

    @Test
    void aceptar_fallaSiSolicitudNoEstaPendiente() {
        solicitud.setEstado(EstadoSolicitud.APROBADA);

        assertThrows(BusinessException.class, () -> service.aceptar(500L, null));
        verify(instanciaEvaluacionService, never()).materializarInicial(any());
    }

    @Test
    void aceptar_fallaSiTrabajoNoEstaAbierto() {
        trabajo.setEstado(EstadoTrabajo.EN_DESARROLLO);

        assertThrows(BusinessException.class, () -> service.aceptar(500L, null));
        verify(instanciaEvaluacionService, never()).materializarInicial(any());
    }

    @Test
    void aceptar_fallaSiTrabajoYaTieneEstudiante() {
        trabajo.setEstudiante(UsuarioFactory.createEstudiante(99L, "otro@x.uy"));

        assertThrows(BusinessException.class, () -> service.aceptar(500L, null));
        verify(instanciaEvaluacionService, never()).materializarInicial(any());
    }
}
