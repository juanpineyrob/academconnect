package com.academconnect.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.academconnect.domain.TemplateEvaluacion;
import com.academconnect.domain.Usuario;
import com.academconnect.domain.Visibilidad;
import com.academconnect.dto.TemplateEvaluacionRequest;
import com.academconnect.dto.TemplateEvaluacionResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.mapper.TemplateEvaluacionMapper;
import com.academconnect.repository.TemplateEvaluacionRepository;
import com.academconnect.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TemplateEvaluacionServiceTests {

    private static final String CRITERIOS_OK = """
            [
              {"codigo":"C1","nombre":"Claridad","tipo":"ESCALA","peso":0.6,"escalaMin":0,"escalaMax":10},
              {"codigo":"C2","nombre":"Originalidad","tipo":"SLIDER","peso":0.4,"escalaMin":0,"escalaMax":10}
            ]
            """;

    @InjectMocks
    private TemplateEvaluacionService service;

    @Mock
    private TemplateEvaluacionRepository repository;

    @Mock
    private TemplateEvaluacionMapper mapper;

    @Mock
    private ApplicationEventPublisher events;

    @Mock
    private UsuarioRepository usuarioRepository;

    private TemplateEvaluacion templateEntity;
    private TemplateEvaluacionResponse templateResponse;

    @BeforeEach
    void setup() {
        templateEntity = new TemplateEvaluacion();
        templateEntity.setNombre("Template TCC");
        templateEntity.setVisibilidad(Visibilidad.PRIVADO);

        templateResponse = new TemplateEvaluacionResponse(
                1L, "Template TCC", null, Visibilidad.PRIVADO, 7L, "Autor",
                CRITERIOS_OK, true, new BigDecimal("6.00"), null, null);

        Mockito.when(mapper.toEntity(Mockito.any())).thenReturn(templateEntity);
        Mockito.when(repository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        Mockito.when(mapper.toResponse(Mockito.any())).thenReturn(templateResponse);
        Mockito.when(usuarioRepository.findById(Mockito.anyLong()))
                .thenReturn(Optional.of(Mockito.mock(Usuario.class)));
    }

    @Test
    void crearShouldReturnResponseWhenCriteriosAndUmbralAreValid() {
        TemplateEvaluacionRequest request = buildRequest(CRITERIOS_OK, new BigDecimal("6.0"));

        Assertions.assertDoesNotThrow(() -> service.crear(request, 7L));
    }

    @Test
    void crearShouldThrowBusinessExceptionWhenCriterioHasNoTipo() {
        String sinTipo = """
                [
                  {"codigo":"C1","nombre":"x","peso":1.0,"escalaMin":0,"escalaMax":10}
                ]
                """;
        Throwable ex = Assertions.assertThrows(BusinessException.class,
                () -> service.crear(buildRequest(sinTipo, new BigDecimal("5.0")), 7L));
        Assertions.assertTrue(ex.getMessage().contains("tipo"));
    }

    @Test
    void crearShouldThrowBusinessExceptionWhenTipoCriterioIsInvalid() {
        String tipoInvalido = """
                [
                  {"codigo":"C1","nombre":"x","tipo":"INEXISTENTE","peso":1.0,"escalaMin":0,"escalaMax":10}
                ]
                """;
        Throwable ex = Assertions.assertThrows(BusinessException.class,
                () -> service.crear(buildRequest(tipoInvalido, new BigDecimal("5.0")), 7L));
        Assertions.assertTrue(ex.getMessage().contains("Tipo de criterio inválido"));
    }

    @Test
    void crearShouldThrowBusinessExceptionWhenSeleccionHasNoOpciones() {
        String seleccionSinOpciones = """
                [
                  {"codigo":"C1","nombre":"x","tipo":"SELECCION","peso":1.0,"escalaMin":0,"escalaMax":10}
                ]
                """;
        Throwable ex = Assertions.assertThrows(BusinessException.class,
                () -> service.crear(buildRequest(seleccionSinOpciones, new BigDecimal("5.0")), 7L));
        Assertions.assertTrue(ex.getMessage().contains("opciones"));
    }

    @Test
    void crearShouldAcceptSeleccionWithOpciones() {
        String seleccionOk = """
                [
                  {"codigo":"C1","nombre":"x","tipo":"SELECCION","peso":1.0,"escalaMin":0,"escalaMax":3,
                   "opciones":["malo","regular","bueno"]}
                ]
                """;
        Assertions.assertDoesNotThrow(
                () -> service.crear(buildRequest(seleccionOk, new BigDecimal("2.0")), 7L));
    }

    @Test
    void crearShouldThrowBusinessExceptionWhenTextoHasNonZeroPeso() {
        String textoPonderado = """
                [
                  {"codigo":"C1","nombre":"x","tipo":"TEXTO","peso":0.5,"escalaMin":0,"escalaMax":10},
                  {"codigo":"C2","nombre":"y","tipo":"ESCALA","peso":0.5,"escalaMin":0,"escalaMax":10}
                ]
                """;
        Throwable ex = Assertions.assertThrows(BusinessException.class,
                () -> service.crear(buildRequest(textoPonderado, new BigDecimal("5.0")), 7L));
        Assertions.assertTrue(ex.getMessage().contains("TEXTO"));
    }

    @Test
    void crearShouldThrowBusinessExceptionWhenUmbralIsOutOfRange() {
        Assertions.assertThrows(BusinessException.class,
                () -> service.crear(buildRequest(CRITERIOS_OK, new BigDecimal("11.0")), 7L));
    }

    @Test
    void crearShouldThrowBusinessExceptionWhenUmbralIsNegative() {
        Assertions.assertThrows(BusinessException.class,
                () -> service.crear(buildRequest(CRITERIOS_OK, new BigDecimal("-1.0")), 7L));
    }

    @Test
    void crearShouldThrowBusinessExceptionWhenSumOfPesosIsNotOne() {
        String pesosMal = """
                [
                  {"codigo":"C1","nombre":"x","tipo":"ESCALA","peso":0.5,"escalaMin":0,"escalaMax":10},
                  {"codigo":"C2","nombre":"y","tipo":"ESCALA","peso":0.3,"escalaMin":0,"escalaMax":10}
                ]
                """;
        Throwable ex = Assertions.assertThrows(BusinessException.class,
                () -> service.crear(buildRequest(pesosMal, new BigDecimal("5.0")), 7L));
        Assertions.assertTrue(ex.getMessage().contains("suma"));
    }

    @Test
    void actualizarShouldThrowWhenCallerIsNotOwnerNorAdmin() {
        var autor = Mockito.mock(Usuario.class);
        Mockito.when(autor.getId()).thenReturn(7L);
        templateEntity.setAutor(autor);
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(templateEntity));

        Assertions.assertThrows(BusinessException.class,
                () -> service.actualizar(1L, buildRequest(CRITERIOS_OK, new BigDecimal("6.0")), 99L, false));
    }

    @Test
    void actualizarShouldSucceedWhenCallerIsOwner() {
        var autor = Mockito.mock(Usuario.class);
        Mockito.when(autor.getId()).thenReturn(7L);
        templateEntity.setAutor(autor);
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(templateEntity));

        Assertions.assertDoesNotThrow(
                () -> service.actualizar(1L, buildRequest(CRITERIOS_OK, new BigDecimal("6.0")), 7L, false));
    }

    @Test
    void actualizarShouldSucceedWhenCallerIsAdminEvenIfNotOwner() {
        var autor = Mockito.mock(Usuario.class);
        Mockito.when(autor.getId()).thenReturn(7L);
        templateEntity.setAutor(autor);
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(templateEntity));

        Assertions.assertDoesNotThrow(
                () -> service.actualizar(1L, buildRequest(CRITERIOS_OK, new BigDecimal("6.0")), 99L, true));
    }

    @Test
    void desactivarShouldThrowWhenCallerIsNotOwnerNorAdmin() {
        var autor = Mockito.mock(Usuario.class);
        Mockito.when(autor.getId()).thenReturn(7L);
        templateEntity.setAutor(autor);
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(templateEntity));

        Assertions.assertThrows(BusinessException.class,
                () -> service.desactivar(1L, 99L, false));
    }

    @Test
    void desactivarShouldSucceedWhenCallerIsOwner() {
        var autor = Mockito.mock(Usuario.class);
        Mockito.when(autor.getId()).thenReturn(7L);
        templateEntity.setAutor(autor);
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(templateEntity));

        Assertions.assertDoesNotThrow(() -> service.desactivar(1L, 7L, false));
    }

    @Test
    void listarVisiblesScopeMiasUsaBuscarMias() {
        Mockito.when(repository.buscarMias(Mockito.any(), Mockito.any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        service.listarVisibles(7L, "MIAS", org.springframework.data.domain.PageRequest.of(0, 12));
        Mockito.verify(repository).buscarMias(Mockito.eq(7L), Mockito.any());
        Mockito.verify(repository, Mockito.never()).buscarPublicas(Mockito.any(), Mockito.any());
    }

    @Test
    void listarVisiblesScopePublicasUsaBuscarPublicas() {
        Mockito.when(repository.buscarPublicas(Mockito.any(), Mockito.any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        service.listarVisibles(7L, "PUBLICAS", org.springframework.data.domain.PageRequest.of(0, 12));
        Mockito.verify(repository).buscarPublicas(Mockito.eq(7L), Mockito.any());
    }

    @Test
    void buscarVisibleShouldThrowWhenNotVisibleForNonAdmin() {
        var autor = Mockito.mock(Usuario.class);
        Mockito.when(autor.getId()).thenReturn(8L);
        templateEntity.setAutor(autor);
        templateEntity.setVisibilidad(Visibilidad.PRIVADO);
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(templateEntity));

        Assertions.assertThrows(BusinessException.class,
                () -> service.buscarVisible(1L, 7L, false));
    }

    @Test
    void buscarVisibleShouldSucceedForOwner() {
        var autor = Mockito.mock(Usuario.class);
        Mockito.when(autor.getId()).thenReturn(7L);
        templateEntity.setAutor(autor);
        templateEntity.setVisibilidad(Visibilidad.PRIVADO);
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(templateEntity));

        Assertions.assertDoesNotThrow(() -> service.buscarVisible(1L, 7L, false));
    }

    private static TemplateEvaluacionRequest buildRequest(String criterios, BigDecimal umbral) {
        return new TemplateEvaluacionRequest(
                "Template", null, Visibilidad.PRIVADO,
                criterios, true, umbral);
    }
}
