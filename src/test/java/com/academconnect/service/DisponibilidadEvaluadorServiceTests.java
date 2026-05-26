package com.academconnect.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

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

import com.academconnect.domain.DisponibilidadEvaluador;
import com.academconnect.domain.Estudiante;
import com.academconnect.dto.DisponibilidadRequest;
import com.academconnect.dto.DisponibilidadResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.factories.DisponibilidadFactory;
import com.academconnect.factories.UsuarioFactory;
import com.academconnect.mapper.DisponibilidadEvaluadorMapper;
import com.academconnect.repository.DisponibilidadEvaluadorRepository;
import com.academconnect.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DisponibilidadEvaluadorServiceTests {

    @InjectMocks
    private DisponibilidadEvaluadorService service;

    @Mock
    private DisponibilidadEvaluadorRepository repository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private DisponibilidadEvaluadorMapper mapper;

    private String email;
    private Estudiante usuario;
    private LocalDate maniana, ayer;
    private BigDecimal horas;
    private DisponibilidadEvaluador disponibilidad;

    @BeforeEach
    void setup() {
        email = "alumno@academconnect.com";
        usuario = UsuarioFactory.createEstudiante(10L, email);
        maniana = LocalDate.now().plusDays(1);
        ayer = LocalDate.now().minusDays(1);
        horas = new BigDecimal("4.00");
        disponibilidad = DisponibilidadFactory.createDisponibilidad(1L, usuario, maniana, horas);

        Mockito.when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        Mockito.when(repository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        Mockito.when(mapper.toResponse(Mockito.any())).thenReturn(
                new DisponibilidadResponse(1L, maniana, horas));
    }

    @Test
    void guardarShouldThrowBusinessExceptionWhenFechaIsInThePast() {
        DisponibilidadRequest request = DisponibilidadFactory.createRequest(ayer, horas);

        Throwable ex = Assertions.assertThrows(BusinessException.class,
                () -> service.guardar(email, request));
        Assertions.assertTrue(ex.getMessage().contains("pasadas"));
    }

    @Test
    void guardarShouldUpdateWhenDisponibilidadAlreadyExists() {
        Mockito.when(repository.findByEvaluadorIdAndFecha(usuario.getId(), maniana))
                .thenReturn(Optional.of(disponibilidad));

        DisponibilidadRequest request = DisponibilidadFactory.createRequest(maniana, new BigDecimal("5.0"));

        Assertions.assertDoesNotThrow(() -> service.guardar(email, request));
        Mockito.verify(repository).save(Mockito.any());
    }

    @Test
    void guardarShouldInsertWhenDisponibilidadDoesNotExist() {
        Mockito.when(repository.findByEvaluadorIdAndFecha(usuario.getId(), maniana))
                .thenReturn(Optional.empty());

        DisponibilidadRequest request = DisponibilidadFactory.createRequest(maniana, horas);

        Assertions.assertDoesNotThrow(() -> service.guardar(email, request));
        Mockito.verify(repository).save(Mockito.any());
    }
}
