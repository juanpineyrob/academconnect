package com.academconnect.service;

import java.math.BigDecimal;

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
import com.academconnect.domain.TemplateScope;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.dto.TemplateEvaluacionRequest;
import com.academconnect.dto.TemplateEvaluacionResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.mapper.TemplateEvaluacionMapper;
import com.academconnect.repository.TemplateEvaluacionRepository;

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

    private TemplateEvaluacion templateEntity;
    private TemplateEvaluacionResponse templateResponse;

    @BeforeEach
    void setup() {
        templateEntity = new TemplateEvaluacion();
        templateEntity.setNombre("Template TCC");
        templateEntity.setScope(TemplateScope.INSTITUCIONAL);

        templateResponse = new TemplateEvaluacionResponse(
                1L, "Template TCC", null, TemplateScope.INSTITUCIONAL, TipoTrabajo.TCC,
                CRITERIOS_OK, true, new BigDecimal("6.00"), null, null);

        Mockito.when(mapper.toEntity(Mockito.any())).thenReturn(templateEntity);
        Mockito.when(repository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        Mockito.when(mapper.toResponse(Mockito.any())).thenReturn(templateResponse);
    }

    @Test
    void crearShouldReturnResponseWhenCriteriosAndUmbralAreValid() {
        TemplateEvaluacionRequest request = buildRequest(CRITERIOS_OK, new BigDecimal("6.0"));

        Assertions.assertDoesNotThrow(() -> service.crear(request));
    }

    @Test
    void crearShouldThrowBusinessExceptionWhenCriterioHasNoTipo() {
        String sinTipo = """
                [
                  {"codigo":"C1","nombre":"x","peso":1.0,"escalaMin":0,"escalaMax":10}
                ]
                """;
        Throwable ex = Assertions.assertThrows(BusinessException.class,
                () -> service.crear(buildRequest(sinTipo, new BigDecimal("5.0"))));
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
                () -> service.crear(buildRequest(tipoInvalido, new BigDecimal("5.0"))));
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
                () -> service.crear(buildRequest(seleccionSinOpciones, new BigDecimal("5.0"))));
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
                () -> service.crear(buildRequest(seleccionOk, new BigDecimal("2.0"))));
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
                () -> service.crear(buildRequest(textoPonderado, new BigDecimal("5.0"))));
        Assertions.assertTrue(ex.getMessage().contains("TEXTO"));
    }

    @Test
    void crearShouldThrowBusinessExceptionWhenUmbralIsOutOfRange() {
        Assertions.assertThrows(BusinessException.class,
                () -> service.crear(buildRequest(CRITERIOS_OK, new BigDecimal("11.0"))));
    }

    @Test
    void crearShouldThrowBusinessExceptionWhenUmbralIsNegative() {
        Assertions.assertThrows(BusinessException.class,
                () -> service.crear(buildRequest(CRITERIOS_OK, new BigDecimal("-1.0"))));
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
                () -> service.crear(buildRequest(pesosMal, new BigDecimal("5.0"))));
        Assertions.assertTrue(ex.getMessage().contains("suma"));
    }

    private static TemplateEvaluacionRequest buildRequest(String criterios, BigDecimal umbral) {
        return new TemplateEvaluacionRequest(
                "Template", null, TemplateScope.INSTITUCIONAL, TipoTrabajo.TCC,
                criterios, true, umbral);
    }
}
