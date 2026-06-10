package com.academconnect.service;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Profesor;
import com.academconnect.dto.PerfilPublicoResponse;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.factories.UsuarioFactory;
import com.academconnect.repository.AreaTematicaRepository;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.repository.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class PerfilServiceTests {

    @InjectMocks
    private PerfilService perfilService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AreaTematicaRepository areaTematicaRepository;

    @Mock
    private TrabajoRepository trabajoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void buscarPerfilPublico_devuelve_datos_publicos_sin_email() {
        Profesor profesor = UsuarioFactory.createProfesor(7L, "prof@academconnect.com");
        profesor.setTitulacion("Dr. en Ciencias");

        Mockito.when(usuarioRepository.findById(7L)).thenReturn(Optional.of(profesor));
        Mockito.when(trabajoRepository.countByOrientadorIdAndEstado(7L, EstadoTrabajo.APROBADO))
                .thenReturn(3L);

        PerfilPublicoResponse result = perfilService.buscarPerfilPublico(7L);

        Assertions.assertEquals("Profesor 7", result.nombre());
        Assertions.assertEquals("PROFESOR", result.rol());
        Assertions.assertEquals("Dr. en Ciencias", result.titulacion());
        Assertions.assertEquals(3, result.trabajosPublicados());
    }

    @Test
    void buscarPerfilPublico_inactivo_lanza_404() {
        Profesor inactivo = UsuarioFactory.createProfesor(5L, "inactivo@academconnect.com");
        inactivo.setActivo(false);

        Mockito.when(usuarioRepository.findById(5L)).thenReturn(Optional.of(inactivo));

        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> perfilService.buscarPerfilPublico(5L));
    }

    @Test
    void buscarPerfilPublico_inexistente_lanza_404() {
        Mockito.when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> perfilService.buscarPerfilPublico(99L));
    }
}
