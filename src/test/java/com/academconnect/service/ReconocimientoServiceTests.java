package com.academconnect.service;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.academconnect.domain.Estudiante;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.Reconocimiento;
import com.academconnect.domain.TipoActividad;
import com.academconnect.dto.ReconocimientoRequest;
import com.academconnect.dto.ReconocimientoResponse;
import com.academconnect.event.ActividadEvent;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.factories.ReconocimientoFactory;
import com.academconnect.factories.UsuarioFactory;
import com.academconnect.mapper.ReconocimientoMapper;
import com.academconnect.repository.ReconocimientoRepository;
import com.academconnect.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReconocimientoServiceTests {

    @InjectMocks
    private ReconocimientoService service;

    @Mock
    private ReconocimientoRepository repository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ApplicationEventPublisher events;

    @Mock
    private ReconocimientoMapper mapper;

    private Long existingDestinatarioId, nonExistingId, reconocimientoId;
    private String otorganteEmail;
    private Estudiante destinatario;
    private Profesor otorgante;
    private Reconocimiento reconocimiento;
    private ReconocimientoRequest request;

    @BeforeEach
    void setup() {
        existingDestinatarioId = 10L;
        nonExistingId = 999L;
        reconocimientoId = 99L;
        otorganteEmail = "admin@academconnect.com";

        destinatario = UsuarioFactory.createEstudiante(existingDestinatarioId, "alumno@academconnect.com");
        otorgante = UsuarioFactory.createProfesor(20L, otorganteEmail);
        reconocimiento = ReconocimientoFactory.createReconocimiento(reconocimientoId, destinatario);
        request = ReconocimientoFactory.createRequest();

        Mockito.when(usuarioRepository.findById(existingDestinatarioId)).thenReturn(Optional.of(destinatario));
        Mockito.when(usuarioRepository.findByEmail(otorganteEmail)).thenReturn(Optional.of(otorgante));
        Mockito.when(usuarioRepository.existsById(existingDestinatarioId)).thenReturn(true);
        Mockito.when(repository.save(Mockito.any())).thenAnswer(i -> {
            Reconocimiento r = i.getArgument(0);
            ReflectionTestUtils.setField(r, "id", reconocimientoId);
            return r;
        });
        Mockito.when(mapper.toResponse(Mockito.any())).thenReturn(
                new ReconocimientoResponse(reconocimientoId, "BECA", "x", 2025, "Admin", Instant.now()));
        Mockito.when(repository.findById(reconocimientoId)).thenReturn(Optional.of(reconocimiento));
        Mockito.when(repository.findById(nonExistingId)).thenReturn(Optional.empty());
        Mockito.when(repository.findByUsuarioIdOrderByAnioDesc(existingDestinatarioId))
                .thenReturn(java.util.List.of(reconocimiento));
    }

    @Test
    void otorgarShouldPublishOtorgadoEventWhenSucceeds() {
        service.otorgar(existingDestinatarioId, otorganteEmail, request);

        ArgumentCaptor<ActividadEvent> captor = ArgumentCaptor.forClass(ActividadEvent.class);
        Mockito.verify(events).publishEvent(captor.capture());
        Assertions.assertEquals(TipoActividad.RECONOCIMIENTO_OTORGADO, captor.getValue().tipo());
    }

    @Test
    void revocarShouldDeleteAndPublishRevocadoEventWhenIdExists() {
        service.revocar(reconocimientoId);

        Mockito.verify(repository).deleteById(reconocimientoId);
        ArgumentCaptor<ActividadEvent> captor = ArgumentCaptor.forClass(ActividadEvent.class);
        Mockito.verify(events).publishEvent(captor.capture());
        Assertions.assertEquals(TipoActividad.RECONOCIMIENTO_REVOCADO, captor.getValue().tipo());
    }

    @Test
    void revocarShouldThrowResourceNotFoundExceptionWhenIdDoesNotExists() {
        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> service.revocar(nonExistingId));
    }

    @Test
    void listarDeUsuarioShouldReturnListWhenUsuarioExists() {
        Assertions.assertDoesNotThrow(() -> service.listarDeUsuario(existingDestinatarioId));
    }

    @Test
    void listarDeUsuarioShouldThrowResourceNotFoundExceptionWhenUsuarioDoesNotExist() {
        Mockito.when(usuarioRepository.existsById(nonExistingId)).thenReturn(false);
        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> service.listarDeUsuario(nonExistingId));
    }
}
