package com.academconnect.service;

import java.util.List;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.academconnect.domain.Documento;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.Trabajo;
import com.academconnect.domain.Versionamiento;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.factories.UsuarioFactory;
import com.academconnect.mapper.VersionamientoMapper;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.repository.VersionamientoRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VersionamientoServiceTests {

    @InjectMocks private VersionamientoService service;
    @Mock private VersionamientoRepository versionamientoRepository;
    @Mock private TrabajoRepository trabajoRepository;
    @Mock private DocumentoService documentoService;
    @Mock private VersionamientoMapper mapper;
    @Mock private ApplicationEventPublisher events;

    private Estudiante estudiante;
    private Trabajo trabajo;
    private Documento documento;
    private MockMultipartFile file;

    @BeforeEach
    void setup() {
        estudiante = UsuarioFactory.createEstudiante(10L, "alumno@x.uy");
        trabajo = new Trabajo();
        ReflectionTestUtils.setField(trabajo, "id", 100L);
        trabajo.setTitulo("Mi trabajo");
        trabajo.setEstudiante(estudiante);

        documento = new Documento();
        ReflectionTestUtils.setField(documento, "id", 500L);
        documento.setStorageKey("k1");
        documento.setNombreOriginal("a.pdf");
        documento.setMimeType("application/pdf");
        documento.setSizeBytes(1L);
        documento.setSha256("sha");

        file = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1, 2, 3});

        Mockito.when(trabajoRepository.findById(100L)).thenReturn(Optional.of(trabajo));
        Mockito.when(trabajoRepository.existsById(100L)).thenReturn(true);
        Mockito.when(documentoService.guardarSiNuevo(Mockito.any())).thenReturn(documento);
        Mockito.when(versionamientoRepository.save(Mockito.any()))
                .thenAnswer(i -> {
                    Versionamiento v = i.getArgument(0);
                    if (v.getId() == null) ReflectionTestUtils.setField(v, "id", 700L + v.getNumeroVersion());
                    return v;
                });
        Mockito.when(mapper.toResponse(Mockito.any())).thenReturn(null);
    }

    @Test
    void crearAsignaSiguienteNumeroVersionYPersiste() {
        Mockito.when(versionamientoRepository.countByTrabajoIdAndDeletedAtIsNull(100L)).thenReturn(0L);
        Mockito.when(versionamientoRepository.findFirstByTrabajoIdOrderByNumeroVersionDesc(100L))
                .thenReturn(Optional.empty());

        service.crear(100L, file, "comentario");

        ArgumentCaptor<Versionamiento> captor = ArgumentCaptor.forClass(Versionamiento.class);
        Mockito.verify(versionamientoRepository).save(captor.capture());
        Versionamiento saved = captor.getValue();
        Assertions.assertEquals(1, saved.getNumeroVersion());
        Assertions.assertEquals("comentario", saved.getComentario());
        Assertions.assertNull(saved.getDeletedAt());
    }

    @Test
    void crearRechazaSiYaHay10Activas() {
        Mockito.when(versionamientoRepository.countByTrabajoIdAndDeletedAtIsNull(100L)).thenReturn(10L);

        Assertions.assertThrows(BusinessException.class,
                () -> service.crear(100L, file, null));
        Mockito.verify(versionamientoRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void reemplazarSoftDeleteaViejaYCreaNueva() {
        Versionamiento vieja = activa(700L, 3);
        Mockito.when(versionamientoRepository.findById(700L)).thenReturn(Optional.of(vieja));
        Mockito.when(versionamientoRepository.countByTrabajoIdAndDeletedAtIsNull(100L)).thenReturn(5L);
        Mockito.when(versionamientoRepository.findFirstByTrabajoIdOrderByNumeroVersionDesc(100L))
                .thenReturn(Optional.of(vieja));

        service.reemplazar(100L, 700L, file, "nuevo comentario", 10L);

        ArgumentCaptor<Versionamiento> captor = ArgumentCaptor.forClass(Versionamiento.class);
        Mockito.verify(versionamientoRepository, Mockito.times(2)).save(captor.capture());
        List<Versionamiento> saves = captor.getAllValues();

        Versionamiento marcada = saves.get(0);
        Assertions.assertEquals(700L, marcada.getId());
        Assertions.assertNotNull(marcada.getDeletedAt());
        Assertions.assertEquals("usuario:10", marcada.getDeletedBy());

        Versionamiento nueva = saves.get(1);
        Assertions.assertEquals(4, nueva.getNumeroVersion());
        Assertions.assertEquals("nuevo comentario", nueva.getComentario());
        Assertions.assertNull(nueva.getDeletedAt());
    }

    @Test
    void reemplazarFallaSiCallerNoEsDuenio() {
        Versionamiento vieja = activa(700L, 1);
        Mockito.when(versionamientoRepository.findById(700L)).thenReturn(Optional.of(vieja));

        Assertions.assertThrows(BusinessException.class,
                () -> service.reemplazar(100L, 700L, file, null, 999L));
    }

    @Test
    void reemplazarFallaSiVersionYaEliminada() {
        Versionamiento vieja = activa(700L, 1);
        vieja.setDeletedAt(java.time.Instant.now());
        Mockito.when(versionamientoRepository.findById(700L)).thenReturn(Optional.of(vieja));

        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> service.reemplazar(100L, 700L, file, null, 10L));
    }

    @Test
    void eliminarMarcaSoftDelete() {
        Versionamiento v = activa(700L, 2);
        Mockito.when(versionamientoRepository.findById(700L)).thenReturn(Optional.of(v));

        service.eliminar(100L, 700L, 10L);

        Assertions.assertNotNull(v.getDeletedAt());
        Assertions.assertEquals("usuario:10", v.getDeletedBy());
        Mockito.verify(versionamientoRepository).save(v);
    }

    @Test
    void eliminarFallaSiCallerNoEsDuenio() {
        Versionamiento v = activa(700L, 1);
        Mockito.when(versionamientoRepository.findById(700L)).thenReturn(Optional.of(v));

        Assertions.assertThrows(BusinessException.class,
                () -> service.eliminar(100L, 700L, 999L));
    }

    @Test
    void eliminarFallaSiYaEstaEliminada() {
        Versionamiento v = activa(700L, 1);
        v.setDeletedAt(java.time.Instant.now());
        Mockito.when(versionamientoRepository.findById(700L)).thenReturn(Optional.of(v));

        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> service.eliminar(100L, 700L, 10L));
    }

    @Test
    void listarPorTrabajoFiltraDeletedPorDefecto() {
        Mockito.when(versionamientoRepository.findByTrabajoIdAndDeletedAtIsNullOrderByNumeroVersionDesc(100L))
                .thenReturn(List.of());

        service.listarPorTrabajo(100L, false);

        Mockito.verify(versionamientoRepository).findByTrabajoIdAndDeletedAtIsNullOrderByNumeroVersionDesc(100L);
        Mockito.verify(versionamientoRepository, Mockito.never())
                .findByTrabajoIdOrderByNumeroVersionDesc(Mockito.anyLong());
    }

    @Test
    void listarPorTrabajoIncluyeDeletedSiSeIndica() {
        Mockito.when(versionamientoRepository.findByTrabajoIdOrderByNumeroVersionDesc(100L))
                .thenReturn(List.of());

        service.listarPorTrabajo(100L, true);

        Mockito.verify(versionamientoRepository).findByTrabajoIdOrderByNumeroVersionDesc(100L);
        Mockito.verify(versionamientoRepository, Mockito.never())
                .findByTrabajoIdAndDeletedAtIsNullOrderByNumeroVersionDesc(Mockito.anyLong());
    }

    private Versionamiento activa(long id, int numero) {
        Versionamiento v = new Versionamiento();
        ReflectionTestUtils.setField(v, "id", id);
        v.setTrabajo(trabajo);
        v.setNumeroVersion(numero);
        v.setDocumento(documento);
        return v;
    }
}
