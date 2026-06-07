package com.academconnect.service;

import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.Trabajo;
import com.academconnect.dto.TrabajoEstudianteRequest;
import com.academconnect.dto.TrabajoResponse;
import com.academconnect.event.ActividadEvent;
import com.academconnect.factories.UsuarioFactory;
import com.academconnect.mapper.TrabajoMapper;
import com.academconnect.repository.AreaTematicaRepository;
import com.academconnect.repository.EstudianteRepository;
import com.academconnect.repository.ProfesorRepository;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.repository.UsuarioRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrabajoServiceEstudianteTests {

    @InjectMocks private TrabajoService service;
    @Mock private TrabajoRepository trabajoRepository;
    @Mock private ProfesorRepository profesorRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AreaTematicaRepository areaTematicaRepository;
    @Mock private TrabajoMapper mapper;
    @Mock private ApplicationEventPublisher events;

    private Estudiante estudiante;
    private TrabajoEstudianteRequest request;

    @BeforeEach
    void setup() {
        estudiante = UsuarioFactory.createEstudiante(10L, "alumno@x.uy");
        Mockito.when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante));
        Mockito.when(areaTematicaRepository.findAllById(Mockito.any())).thenReturn(List.of());
        Mockito.when(trabajoRepository.save(Mockito.any(Trabajo.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        // stub mapper.toResponse to return a non-null TrabajoResponse with any args
        Mockito.when(mapper.toResponse(Mockito.any(Trabajo.class)))
                .thenReturn(null); // will be replaced after you check the TrabajoResponse signature

        request = new TrabajoEstudianteRequest(
                "Mi TCC", "Resumen", TipoTrabajo.TCC, Set.of(),
                List.of("Seguridad","DevSecOps","CI/CD"));
    }

    @Test
    void crearPorEstudianteSeteaEstadoBorradorYVinculaEstudiante() {
        service.crearPorEstudiante(request, 10L);

        ArgumentCaptor<Trabajo> captor = ArgumentCaptor.forClass(Trabajo.class);
        Mockito.verify(trabajoRepository).save(captor.capture());
        Trabajo t = captor.getValue();
        Assertions.assertEquals(EstadoTrabajo.BORRADOR, t.getEstado());
        Assertions.assertEquals(estudiante, t.getEstudiante());
        Assertions.assertNull(t.getOrientador());
        Assertions.assertEquals(List.of("seguridad","devsecops","ci/cd"), t.getKeywords());
    }

    @Test
    void crearPorEstudiantePublicaEventoTrabajoCreado() {
        service.crearPorEstudiante(request, 10L);
        ArgumentCaptor<ActividadEvent> captor = ArgumentCaptor.forClass(ActividadEvent.class);
        Mockito.verify(events).publishEvent(captor.capture());
        // recursoTipo accessor: ActividadEvent is a record with `recursoTipo` field
        Assertions.assertEquals("TRABAJO", captor.getValue().recursoTipo());
    }

    @Test
    void actualizarFallaSiUsuarioNoEsDuenio() {
        Trabajo t = new Trabajo();
        t.setId(99L);
        t.setEstado(EstadoTrabajo.BORRADOR);
        t.setEstudiante(estudiante);
        Mockito.when(trabajoRepository.findById(99L)).thenReturn(Optional.of(t));

        Assertions.assertThrows(com.academconnect.exception.BusinessException.class,
                () -> service.actualizarBorradorPorEstudiante(99L, request, /* otroEstudianteId */ 999L));
    }

    @Test
    void actualizarFallaSiNoEstaEnBorrador() {
        Trabajo t = new Trabajo();
        t.setId(100L);
        t.setEstado(EstadoTrabajo.EN_DESARROLLO);
        t.setEstudiante(estudiante);
        Mockito.when(trabajoRepository.findById(100L)).thenReturn(Optional.of(t));

        Assertions.assertThrows(com.academconnect.exception.BusinessException.class,
                () -> service.actualizarBorradorPorEstudiante(100L, request, 10L));
    }

    @Test
    void actualizarOkSobreescribeCampos() {
        Trabajo t = new Trabajo();
        t.setId(200L);
        t.setEstado(EstadoTrabajo.BORRADOR);
        t.setEstudiante(estudiante);
        Mockito.when(trabajoRepository.findById(200L)).thenReturn(Optional.of(t));

        service.actualizarBorradorPorEstudiante(200L, request, 10L);
        Assertions.assertEquals("Mi TCC", t.getTitulo());
        Assertions.assertEquals(TipoTrabajo.TCC, t.getTipo());
        Assertions.assertEquals(List.of("seguridad","devsecops","ci/cd"), t.getKeywords());
    }
}
