package com.academconnect.service;

import java.math.BigDecimal;
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
import org.springframework.context.ApplicationEventPublisher;

import com.academconnect.domain.Asignacion;
import com.academconnect.domain.EstadoAsignacion;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.TemplateEvaluacion;
import com.academconnect.domain.Visibilidad;
import com.academconnect.exception.BusinessException;
import com.academconnect.mapper.AsignacionMapper;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.ConflictoInteresRepository;
import com.academconnect.repository.TemplateEvaluacionRepository;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.repository.VersionamientoRepository;

@ExtendWith(MockitoExtension.class)
class AsignacionServiceTests {

    @Mock private AsignacionRepository asignacionRepository;
    @Mock private TrabajoRepository trabajoRepository;
    @Mock private VersionamientoRepository versionamientoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TemplateEvaluacionRepository templateRepository;
    @Mock private ConflictoInteresRepository conflictoRepository;
    @Mock private AsignacionMapper mapper;
    @Mock private ApplicationEventPublisher events;

    @InjectMocks private AsignacionService service;

    private static final String EMAIL = "eval@academ.uy";

    private Profesor evaluador;
    private Asignacion asignacion;

    @BeforeEach
    void setup() {
        evaluador = new Profesor();
        evaluador.setId(30L);

        asignacion = new Asignacion();
        asignacion.setId(7L);
        asignacion.setEvaluador(evaluador);
        asignacion.setEstado(EstadoAsignacion.ACTIVA);

        Mockito.lenient().when(asignacionRepository.findById(7L)).thenReturn(Optional.of(asignacion));
        Mockito.lenient().when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(evaluador));
        Mockito.lenient().when(asignacionRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
    }

    private TemplateEvaluacion template(boolean porDefecto, Visibilidad visibilidad, Profesor autor) {
        var t = new TemplateEvaluacion();
        t.setId(1L);
        t.setActivo(true);
        t.setEsPorDefecto(porDefecto);
        t.setVisibilidad(visibilidad);
        t.setAutor(autor);
        t.setCriterios("[{\"codigo\":\"c1\",\"peso\":1}]");
        t.setUmbralAprobacion(BigDecimal.valueOf(6));
        return t;
    }

    @Test
    void seleccionarRubrica_conTemplatePropio_congelaSnapshot() {
        Mockito.when(templateRepository.findById(1L))
                .thenReturn(Optional.of(template(false, Visibilidad.PRIVADO, evaluador)));

        service.seleccionarRubrica(7L, 1L, EMAIL);

        ArgumentCaptor<Asignacion> cap = ArgumentCaptor.forClass(Asignacion.class);
        Mockito.verify(asignacionRepository).save(cap.capture());
        Assertions.assertNotNull(cap.getValue().getTemplateSnapshot());
        Assertions.assertTrue(cap.getValue().getTemplateSnapshot().contains("umbralAprobacion"));
    }

    @Test
    void seleccionarRubrica_sinTemplate_usaPorDefecto() {
        Mockito.when(templateRepository.findFirstByEsPorDefectoTrueAndActivoTrue())
                .thenReturn(Optional.of(template(true, Visibilidad.PUBLICO, null)));

        service.seleccionarRubrica(7L, null, EMAIL);

        Mockito.verify(templateRepository).findFirstByEsPorDefectoTrueAndActivoTrue();
        Mockito.verify(asignacionRepository).save(Mockito.any());
    }

    @Test
    void seleccionarRubrica_templatePrivadoAjeno_falla() {
        var otro = new Profesor();
        otro.setId(99L);
        Mockito.when(templateRepository.findById(1L))
                .thenReturn(Optional.of(template(false, Visibilidad.PRIVADO, otro)));

        Assertions.assertThrows(BusinessException.class, () -> service.seleccionarRubrica(7L, 1L, EMAIL));
        Mockito.verify(asignacionRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void seleccionarRubrica_callerNoEsEvaluador_falla() {
        var otro = new Profesor();
        otro.setId(99L);
        Mockito.when(usuarioRepository.findByEmail("intruso@academ.uy")).thenReturn(Optional.of(otro));

        Assertions.assertThrows(BusinessException.class,
                () -> service.seleccionarRubrica(7L, 1L, "intruso@academ.uy"));
        Mockito.verify(asignacionRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void seleccionarRubrica_asignacionNoActiva_falla() {
        asignacion.setEstado(EstadoAsignacion.COMPLETADA);

        Assertions.assertThrows(BusinessException.class, () -> service.seleccionarRubrica(7L, 1L, EMAIL));
        Mockito.verify(asignacionRepository, Mockito.never()).save(Mockito.any());
    }
}
