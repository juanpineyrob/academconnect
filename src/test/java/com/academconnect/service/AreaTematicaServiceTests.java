package com.academconnect.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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

import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.ThesaurusOrigen;
import com.academconnect.dto.AreaTematicaRequest;
import com.academconnect.dto.AreaTematicaResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.mapper.AreaTematicaMapper;
import com.academconnect.repository.AreaTematicaRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AreaTematicaServiceTests {

    @InjectMocks private AreaTematicaService service;
    @Mock private AreaTematicaRepository repository;
    @Mock private AreaTematicaMapper mapper;

    @BeforeEach
    void setup() {
        Mockito.when(repository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        Mockito.when(mapper.toResponse(Mockito.any())).thenAnswer(i -> {
            AreaTematica a = i.getArgument(0);
            return new AreaTematicaResponse(a.getId(), a.getCodigoExterno(), a.getNombre(),
                    a.getParent() == null ? null : a.getParent().getId(), a.getThesaurusOrigen(), a.isActivo());
        });
    }

    private AreaTematica area(String nombre, boolean activo) {
        AreaTematica a = new AreaTematica();
        a.setNombre(nombre);
        a.setThesaurusOrigen(ThesaurusOrigen.INTERNO);
        a.setActivo(activo);
        return a;
    }

    @Test
    void crearTrimeaNombreYActivaPorDefecto() {
        service.crear(new AreaTematicaRequest("  Inteligencia Artificial  ", " EXT1 ", ThesaurusOrigen.CNPQ, null));
        ArgumentCaptor<AreaTematica> cap = ArgumentCaptor.forClass(AreaTematica.class);
        Mockito.verify(repository).save(cap.capture());
        Assertions.assertEquals("Inteligencia Artificial", cap.getValue().getNombre());
        Assertions.assertEquals("EXT1", cap.getValue().getCodigoExterno());
        Assertions.assertEquals(ThesaurusOrigen.CNPQ, cap.getValue().getThesaurusOrigen());
        Assertions.assertNull(cap.getValue().getParent());
        Assertions.assertTrue(cap.getValue().isActivo());
    }

    @Test
    void crearConCodigoVacioGuardaNull() {
        service.crear(new AreaTematicaRequest("X", "   ", ThesaurusOrigen.INTERNO, null));
        ArgumentCaptor<AreaTematica> cap = ArgumentCaptor.forClass(AreaTematica.class);
        Mockito.verify(repository).save(cap.capture());
        Assertions.assertNull(cap.getValue().getCodigoExterno());
    }

    @Test
    void crearConParentLoCarga() {
        AreaTematica parent = area("Padre", true);
        parent.setId(5L);
        Mockito.when(repository.findById(5L)).thenReturn(Optional.of(parent));
        service.crear(new AreaTematicaRequest("Sub", null, ThesaurusOrigen.INTERNO, 5L));
        ArgumentCaptor<AreaTematica> cap = ArgumentCaptor.forClass(AreaTematica.class);
        Mockito.verify(repository).save(cap.capture());
        Assertions.assertEquals(parent, cap.getValue().getParent());
    }

    @Test
    void actualizarAplicaCambios() {
        AreaTematica area = area("viejo", true);
        area.setId(1L);
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(area));
        service.actualizar(1L, new AreaTematicaRequest("nuevo", null, ThesaurusOrigen.ACM_CCS, null));
        Assertions.assertEquals("nuevo", area.getNombre());
        Assertions.assertEquals(ThesaurusOrigen.ACM_CCS, area.getThesaurusOrigen());
    }

    @Test
    void actualizarConParentIgualASiMismoFalla() {
        AreaTematica area = area("x", true);
        area.setId(1L);
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(area));
        Assertions.assertThrows(BusinessException.class,
                () -> service.actualizar(1L, new AreaTematicaRequest("x", null, ThesaurusOrigen.INTERNO, 1L)));
    }

    @Test
    void setActivoDesactiva() {
        AreaTematica area = area("y", true);
        area.setId(2L);
        Mockito.when(repository.findById(2L)).thenReturn(Optional.of(area));
        service.setActivo(2L, false);
        Assertions.assertFalse(area.isActivo());
    }

    @Test
    void buscarMapeaResultadosIncluyendoInactivas() {
        Mockito.when(repository.buscarAdmin(Mockito.any(), Mockito.any()))
                .thenReturn(new PageImpl<>(List.of(area("alfa", false))));
        var res = service.buscar("al", PageRequest.of(0, 10));
        Assertions.assertEquals(1, res.getTotalElements());
        Assertions.assertEquals("alfa", res.getContent().get(0).nombre());
        Assertions.assertFalse(res.getContent().get(0).activo());
    }
}
