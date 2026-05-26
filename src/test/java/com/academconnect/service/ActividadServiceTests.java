package com.academconnect.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import com.academconnect.domain.Actividad;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.VisibilidadActividad;
import com.academconnect.event.ActividadEvent;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.factories.UsuarioFactory;
import com.academconnect.mapper.ActividadMapper;
import com.academconnect.repository.ActividadRepository;
import com.academconnect.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ActividadServiceTests {

    @InjectMocks
    private ActividadService service;

    @Mock
    private ActividadRepository repository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ActividadMapper mapper;

    private String existingEmail, nonExistingEmail;
    private ActividadEvent event;

    @BeforeEach
    void setup() {
        existingEmail = "alumno@academconnect.com";
        nonExistingEmail = "nope@academconnect.com";

        event = ActividadEvent.of(
                TipoActividad.TRABAJO_CREADO, 1L, "TRABAJO", 99L,
                Map.of("titulo", "X"), VisibilidadActividad.PUBLICA, List.of(1L, 2L));

        Mockito.when(usuarioRepository.findByEmail(existingEmail))
                .thenReturn(Optional.of(UsuarioFactory.createEstudiante(10L, existingEmail)));
        Mockito.when(usuarioRepository.findByEmail(nonExistingEmail))
                .thenReturn(Optional.empty());
    }

    @Test
    void persistirShouldSaveActividadWhenEventIsValid() {
        service.persistir(event);

        ArgumentCaptor<Actividad> captor = ArgumentCaptor.forClass(Actividad.class);
        Mockito.verify(repository).save(captor.capture());
        Actividad a = captor.getValue();
        Assertions.assertEquals(TipoActividad.TRABAJO_CREADO, a.getTipo());
        Assertions.assertEquals(1L, a.getActorId());
        Assertions.assertEquals(99L, a.getRecursoId());
        Assertions.assertTrue(a.getPayload().contains("titulo"));
        Assertions.assertEquals(VisibilidadActividad.PUBLICA, a.getVisibilidad());
        Assertions.assertEquals(List.of(1L, 2L), a.getParticipantesIds());
    }

    @Test
    void persistirShouldNotPropagateExceptionWhenRepositoryFails() {
        Mockito.when(repository.save(Mockito.any())).thenThrow(new RuntimeException("DB caída"));

        Assertions.assertDoesNotThrow(() -> service.persistir(event));
    }

    @Test
    void feedShouldThrowResourceNotFoundExceptionWhenEmailDoesNotExist() {
        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> service.feed(nonExistingEmail, 20));
    }

    @Test
    void feedShouldReturnListWhenEmailExists() {
        Mockito.when(repository.feedDelUsuario(Mockito.anyLong(), Mockito.any()))
                .thenReturn(List.of());
        Assertions.assertDoesNotThrow(() -> service.feed(existingEmail, 20));
    }
}
