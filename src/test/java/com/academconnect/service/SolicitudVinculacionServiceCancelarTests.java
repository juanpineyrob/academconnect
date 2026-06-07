package com.academconnect.service;

import com.academconnect.domain.EstadoSolicitud;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.SolicitudVinculacion;
import com.academconnect.domain.Trabajo;
import com.academconnect.exception.BusinessException;
import com.academconnect.factories.UsuarioFactory;
import com.academconnect.mapper.SolicitudVinculacionMapper;
import com.academconnect.repository.EstudianteRepository;
import com.academconnect.repository.SolicitudVinculacionRepository;
import com.academconnect.repository.TrabajoRepository;
import org.junit.jupiter.api.Assertions;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudVinculacionServiceCancelarTests {

    @InjectMocks private SolicitudVinculacionService service;
    @Mock private SolicitudVinculacionRepository solicitudRepository;
    @Mock private TrabajoRepository trabajoRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private SolicitudVinculacionMapper mapper;
    @Mock private ApplicationEventPublisher events;
    @Mock private TrabajoService trabajoService;

    private Estudiante estudiante;
    private Trabajo trabajo;
    private SolicitudVinculacion solicitud;

    @BeforeEach
    void setup() {
        estudiante = UsuarioFactory.createEstudiante(10L, "alumno@x.uy");
        trabajo = new Trabajo();
        trabajo.setId(100L);
        trabajo.setTitulo("X");
        solicitud = new SolicitudVinculacion();
        solicitud.setId(500L);
        solicitud.setTrabajo(trabajo);
        solicitud.setEstudiante(estudiante);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        Mockito.when(solicitudRepository.findById(500L)).thenReturn(Optional.of(solicitud));
        Mockito.when(solicitudRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        Mockito.when(mapper.toResponse(Mockito.any())).thenReturn(null);
    }

    @Test
    void cancelarOkMarcaCancelada() {
        service.cancelar(500L, 10L);
        Assertions.assertEquals(EstadoSolicitud.CANCELADA, solicitud.getEstado());
        Assertions.assertNotNull(solicitud.getResueltaEn());
    }

    @Test
    void cancelarFallaSiNoEsDuenio() {
        Assertions.assertThrows(BusinessException.class, () -> service.cancelar(500L, 999L));
    }

    @Test
    void cancelarFallaSiNoEstaPendiente() {
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        Assertions.assertThrows(BusinessException.class, () -> service.cancelar(500L, 10L));
    }
}
