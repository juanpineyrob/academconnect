package com.academconnect.service;

import com.academconnect.domain.EstadoAsignacion;
import com.academconnect.domain.EstadoInvitacion;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.Externo;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.SolicitudEvaluacion;
import com.academconnect.domain.TemplateEvaluacion;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.TipoTrabajoConfig;
import com.academconnect.domain.Trabajo;
import com.academconnect.domain.Versionamiento;
import com.academconnect.dto.AsignacionRequest;
import com.academconnect.dto.SolicitudEvaluacionRequest;
import com.academconnect.exception.BusinessException;
import com.academconnect.factories.UsuarioFactory;
import com.academconnect.mapper.SolicitudEvaluacionMapper;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.CoorientadorRepository;
import com.academconnect.repository.ConflictoInteresRepository;
import com.academconnect.repository.SolicitudEvaluacionRepository;
import com.academconnect.repository.TemplateEvaluacionRepository;
import com.academconnect.repository.TipoTrabajoConfigRepository;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.repository.VersionamientoRepository;
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

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudEvaluacionServiceTests {

    @InjectMocks private SolicitudEvaluacionService service;
    @Mock private SolicitudEvaluacionRepository repository;
    @Mock private TrabajoRepository trabajoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AsignacionRepository asignacionRepository;
    @Mock private VersionamientoRepository versionamientoRepository;
    @Mock private TipoTrabajoConfigRepository tipoTrabajoConfigRepository;
    @Mock private TemplateEvaluacionRepository templateRepository;
    @Mock private CoorientadorRepository coorientadorRepository;
    @Mock private ConflictoInteresRepository conflictoRepository;
    @Mock private AsignacionService asignacionService;
    @Mock private SolicitudEvaluacionMapper mapper;
    @Mock private InstanciaEvaluacionService instanciaEvaluacionService;

    private Estudiante estudiante;
    private Profesor orientador;
    private Profesor evaluador;
    private Externo evaluadorExterno;
    private Trabajo trabajo;
    private Versionamiento version;
    private TemplateEvaluacion templateDefault;

    @BeforeEach
    void setup() {
        estudiante = UsuarioFactory.createEstudiante(10L, "alumno@x.uy");
        orientador = UsuarioFactory.createProfesor(20L, "orientador@x.uy");
        evaluador = UsuarioFactory.createProfesor(30L, "eval@x.uy");
        evaluadorExterno = UsuarioFactory.createExterno(40L, "ext@x.uy");

        trabajo = new Trabajo();
        trabajo.setId(100L);
        trabajo.setTitulo("Tesis");
        trabajo.setTipo(TipoTrabajo.TCC);
        trabajo.setEstado(EstadoTrabajo.EN_DESARROLLO);
        trabajo.setEstudiante(estudiante);
        trabajo.setOrientador(orientador);

        version = new Versionamiento();
        version.setId(500L);

        templateDefault = new TemplateEvaluacion();
        templateDefault.setId(1L);

        var config = new TipoTrabajoConfig();
        config.setTipo(TipoTrabajo.TCC);
        config.setEvaluadoresDefault(3);

        Mockito.when(trabajoRepository.findById(100L)).thenReturn(Optional.of(trabajo));
        Mockito.when(usuarioRepository.findById(30L)).thenReturn(Optional.of(evaluador));
        Mockito.when(usuarioRepository.findById(40L)).thenReturn(Optional.of(evaluadorExterno));
        Mockito.when(tipoTrabajoConfigRepository.findById(TipoTrabajo.TCC)).thenReturn(Optional.of(config));
        Mockito.when(versionamientoRepository.findFirstByTrabajoIdOrderByNumeroVersionDesc(100L))
                .thenReturn(Optional.of(version));
        Mockito.when(templateRepository.findFirstByEsPorDefectoTrueAndActivoTrue())
                .thenReturn(Optional.of(templateDefault));
        Mockito.when(coorientadorRepository.findByTrabajoId(100L)).thenReturn(List.of());
        Mockito.when(conflictoRepository.existsByTrabajoIdAndEvaluadorId(100L, 30L)).thenReturn(false);
        Mockito.when(asignacionRepository.countByTrabajoIdAndEstado(100L, EstadoAsignacion.ACTIVA)).thenReturn(0L);
        Mockito.when(repository.countByTrabajoIdAndEstado(100L, EstadoInvitacion.PENDIENTE)).thenReturn(0L);
        // ronda única por defecto — sin instancia activa → usa evaluadoresDefault
        Mockito.when(instanciaEvaluacionService.instanciaActiva(100L)).thenReturn(java.util.Optional.empty());
        Mockito.when(repository.existsByTrabajoIdAndInvitadoIdAndEstado(
                Mockito.eq(100L), Mockito.anyLong(), Mockito.eq(EstadoInvitacion.PENDIENTE))).thenReturn(false);
        Mockito.when(repository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
    }

    private SolicitudEvaluacionRequest req(Long usuarioId) {
        return new SolicitudEvaluacionRequest(100L, usuarioId, "te invito a evaluar");
    }

    @Test
    void crear_okConProfesor() {
        service.crear(req(30L), estudiante.getId());
        ArgumentCaptor<SolicitudEvaluacion> cap = ArgumentCaptor.forClass(SolicitudEvaluacion.class);
        Mockito.verify(repository).save(cap.capture());
        Assertions.assertEquals(EstadoInvitacion.PENDIENTE, cap.getValue().getEstado());
        Assertions.assertEquals(30L, cap.getValue().getInvitado().getId());
    }

    @Test
    void crear_okConExterno() {
        service.crear(req(40L), estudiante.getId());
        Mockito.verify(repository).save(Mockito.any());
    }

    @Test
    void crear_fallaSiNoEsDueno() {
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req(30L), 999L));
    }

    @Test
    void crear_fallaSinOrientador() {
        trabajo.setOrientador(null);
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req(30L), estudiante.getId()));
    }

    @Test
    void crear_fallaSiFinalizado() {
        trabajo.setEstado(EstadoTrabajo.APROBADO);
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req(30L), estudiante.getId()));
    }

    @Test
    void crear_fallaSinVersion() {
        Mockito.when(versionamientoRepository.findFirstByTrabajoIdOrderByNumeroVersionDesc(100L))
                .thenReturn(Optional.empty());
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req(30L), estudiante.getId()));
    }

    @Test
    void crear_fallaSiBancaCompleta() {
        Mockito.when(asignacionRepository.countByTrabajoIdAndEstado(100L, EstadoAsignacion.ACTIVA)).thenReturn(2L);
        Mockito.when(repository.countByTrabajoIdAndEstado(100L, EstadoInvitacion.PENDIENTE)).thenReturn(1L); // 2+1=3=N
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req(30L), estudiante.getId()));
    }

    @Test
    void crear_fallaSiInvitadoEsOrientador() {
        Mockito.when(usuarioRepository.findById(20L)).thenReturn(Optional.of(orientador));
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req(20L), estudiante.getId()));
    }

    @Test
    void crear_fallaSiConflictoInteres() {
        Mockito.when(conflictoRepository.existsByTrabajoIdAndEvaluadorId(100L, 30L)).thenReturn(true);
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req(30L), estudiante.getId()));
    }

    @Test
    void aceptar_creaAsignacionViaAsignacionService() {
        SolicitudEvaluacion s = new SolicitudEvaluacion();
        s.setId(7L);
        s.setTrabajo(trabajo);
        s.setInvitado(evaluador);
        s.setEstado(EstadoInvitacion.PENDIENTE);
        Mockito.when(repository.findById(7L)).thenReturn(Optional.of(s));

        service.aceptar(7L, null, evaluador.getId());

        Assertions.assertEquals(EstadoInvitacion.ACEPTADA, s.getEstado());
        ArgumentCaptor<AsignacionRequest> cap = ArgumentCaptor.forClass(AsignacionRequest.class);
        Mockito.verify(asignacionService).crear(cap.capture());
        Assertions.assertEquals(100L, cap.getValue().trabajoId());
        Assertions.assertEquals(500L, cap.getValue().versionamientoId());
        Assertions.assertEquals(30L, cap.getValue().evaluadorId());
        Assertions.assertEquals(1L, cap.getValue().templateEvaluacionId());
    }

    @Test
    void aceptar_fallaSiNoEsElInvitado() {
        SolicitudEvaluacion s = new SolicitudEvaluacion();
        s.setId(7L);
        s.setTrabajo(trabajo);
        s.setInvitado(evaluador);
        s.setEstado(EstadoInvitacion.PENDIENTE);
        Mockito.when(repository.findById(7L)).thenReturn(Optional.of(s));
        Assertions.assertThrows(BusinessException.class, () -> service.aceptar(7L, null, 999L));
        Mockito.verify(asignacionService, Mockito.never()).crear(Mockito.any());
    }

    @Test
    void rechazar_marcaRechazadaSinAsignacion() {
        SolicitudEvaluacion s = new SolicitudEvaluacion();
        s.setId(7L);
        s.setTrabajo(trabajo);
        s.setInvitado(evaluador);
        s.setEstado(EstadoInvitacion.PENDIENTE);
        Mockito.when(repository.findById(7L)).thenReturn(Optional.of(s));
        service.rechazar(7L, null, evaluador.getId());
        Assertions.assertEquals(EstadoInvitacion.RECHAZADA, s.getEstado());
        Mockito.verify(asignacionService, Mockito.never()).crear(Mockito.any());
    }

    @Test
    void cancelar_soloDueno() {
        SolicitudEvaluacion s = new SolicitudEvaluacion();
        s.setId(7L);
        s.setTrabajo(trabajo);
        s.setInvitado(evaluador);
        s.setEstado(EstadoInvitacion.PENDIENTE);
        Mockito.when(repository.findById(7L)).thenReturn(Optional.of(s));
        Assertions.assertThrows(BusinessException.class, () -> service.cancelar(7L, 999L));
        service.cancelar(7L, estudiante.getId());
        Assertions.assertEquals(EstadoInvitacion.CANCELADA, s.getEstado());
    }

    @Test
    void crear_fallaSiInvitadoEsCoorientador() {
        var co = new com.academconnect.domain.Coorientador();
        co.setUsuario(evaluador);
        Mockito.when(coorientadorRepository.findByTrabajoId(100L)).thenReturn(List.of(co));
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req(30L), estudiante.getId()));
    }

    @Test
    void rechazar_fallaSiNoEsElInvitado() {
        SolicitudEvaluacion s = new SolicitudEvaluacion();
        s.setId(7L);
        s.setTrabajo(trabajo);
        s.setInvitado(evaluador);
        s.setEstado(EstadoInvitacion.PENDIENTE);
        Mockito.when(repository.findById(7L)).thenReturn(Optional.of(s));
        Assertions.assertThrows(BusinessException.class, () -> service.rechazar(7L, null, 999L));
    }

    @Test
    void aceptar_fallaSiBancaCompleta() {
        SolicitudEvaluacion s = new SolicitudEvaluacion();
        s.setId(7L);
        s.setTrabajo(trabajo);
        s.setInvitado(evaluador);
        s.setEstado(EstadoInvitacion.PENDIENTE);
        Mockito.when(repository.findById(7L)).thenReturn(Optional.of(s));
        Mockito.when(asignacionRepository.countByTrabajoIdAndEstado(100L, EstadoAsignacion.ACTIVA)).thenReturn(3L);
        Assertions.assertThrows(BusinessException.class, () -> service.aceptar(7L, null, evaluador.getId()));
        Mockito.verify(asignacionService, Mockito.never()).crear(Mockito.any());
    }

    @Test
    void crear_dimensionaBancaPorInstanciaActiva() {
        // instancia activa con config de 2 evaluadores; ya hay 2 asignaciones activas → banca llena → error
        var cfg = new com.academconnect.domain.InstanciaEvaluacionConfig();
        cfg.setEvaluadoresRequeridos(2);
        var ie = new com.academconnect.domain.InstanciaEvaluacion();
        ie.setInstanciaConfig(cfg);
        Mockito.when(instanciaEvaluacionService.instanciaActiva(100L)).thenReturn(java.util.Optional.of(ie));
        Mockito.when(asignacionRepository.countByTrabajoIdAndEstado(100L, EstadoAsignacion.ACTIVA)).thenReturn(2L);
        Mockito.when(repository.countByTrabajoIdAndEstado(100L, EstadoInvitacion.PENDIENTE)).thenReturn(0L);
        // banca llena (2 activas para N=2 de la instancia) → error
        Assertions.assertThrows(BusinessException.class, () -> service.crear(req(30L), estudiante.getId()));
    }
}
