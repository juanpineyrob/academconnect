package com.academconnect.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.NivelExperticia;
import com.academconnect.domain.Profesor;
import com.academconnect.dto.PerfilPublicoResponse;
import com.academconnect.dto.PerfilResponse;
import com.academconnect.dto.UsuarioAreaTematicaResponse;
import com.academconnect.dto.UsuarioAreasRequest;
import com.academconnect.exception.BusinessException;
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

    @Test
    void toPerfilResponse_estudiante_devuelve_areas_derivadas_de_trabajos_aprobados() {
        Estudiante estudiante = UsuarioFactory.createEstudiante(10L, "alumno@academconnect.com");

        List<UsuarioAreaTematicaResponse> areasDerivadas = List.of(
                new UsuarioAreaTematicaResponse(1L, "Inteligencia Artificial", null),
                new UsuarioAreaTematicaResponse(2L, "Redes de Computadores", null));

        Mockito.when(usuarioRepository.findById(10L)).thenReturn(Optional.of(estudiante));
        Mockito.when(trabajoRepository.countByEstudianteIdAndEstado(10L, EstadoTrabajo.APROBADO))
                .thenReturn(2L);
        Mockito.when(trabajoRepository.areasDerivadas(10L)).thenReturn(areasDerivadas);

        PerfilPublicoResponse result = perfilService.buscarPerfilPublico(10L);

        Assertions.assertEquals(2, result.areas().size());
        Assertions.assertNull(result.areas().get(0).nivelExperticia());
        Assertions.assertNull(result.areas().get(1).nivelExperticia());
    }

    @Test
    void actualizarAreas_estudiante_lanza_excepcion_de_negocio() {
        Estudiante estudiante = UsuarioFactory.createEstudiante(10L, "alumno@academconnect.com");

        Mockito.when(usuarioRepository.findByEmail("alumno@academconnect.com"))
                .thenReturn(Optional.of(estudiante));

        UsuarioAreasRequest request = new UsuarioAreasRequest(List.of());

        Assertions.assertThrows(BusinessException.class,
                () -> perfilService.actualizarAreas("alumno@academconnect.com", request));
    }

    @Test
    void actualizarAreas_profesor_funciona_normalmente() {
        Profesor profesor = UsuarioFactory.createProfesor(7L, "prof@academconnect.com");

        com.academconnect.domain.AreaTematica area = new com.academconnect.domain.AreaTematica();
        org.springframework.test.util.ReflectionTestUtils.setField(area, "id", 1L);
        area.setNombre("Machine Learning");

        Mockito.when(usuarioRepository.findByEmail("prof@academconnect.com"))
                .thenReturn(Optional.of(profesor));
        Mockito.when(areaTematicaRepository.findById(1L)).thenReturn(Optional.of(area));
        Mockito.when(usuarioRepository.save(Mockito.any())).thenReturn(profesor);

        UsuarioAreasRequest request = new UsuarioAreasRequest(List.of(
                new UsuarioAreasRequest.AreaConNivelRequest(1L, NivelExperticia.ALTO)));

        List<UsuarioAreaTematicaResponse> result =
                perfilService.actualizarAreas("prof@academconnect.com", request);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Machine Learning", result.get(0).areaNombre());
        Assertions.assertEquals(NivelExperticia.ALTO, result.get(0).nivelExperticia());
    }
}
