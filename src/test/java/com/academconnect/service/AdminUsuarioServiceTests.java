package com.academconnect.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.academconnect.domain.Administrador;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.Externo;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.Rol;
import com.academconnect.domain.Usuario;
import com.academconnect.dto.AdminUsuarioCreateRequest;
import com.academconnect.dto.AdminUsuarioUpdateRequest;
import com.academconnect.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminUsuarioServiceTests {

    @InjectMocks private AdminUsuarioService service;
    @Mock private com.academconnect.repository.UsuarioRepository repository;
    @Mock private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        Mockito.when(repository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        Mockito.when(passwordEncoder.encode(Mockito.any())).thenAnswer(i -> "hashed:" + i.getArgument(0));
    }

    private AdminUsuarioCreateRequest crearReq(Rol rol, String email, String institucion, String titulo) {
        return new AdminUsuarioCreateRequest(rol, email, "MAT-" + email, "password123", "Nombre", 30, "Montevideo",
                "Doctorado", "Titular", institucion, titulo);
    }

    @Test
    void crearEstudianteHasheaPasswordYActiva() {
        var resp = service.crear(crearReq(Rol.ESTUDIANTE, "e@x.uy", null, null));
        ArgumentCaptor<Usuario> cap = ArgumentCaptor.forClass(Usuario.class);
        Mockito.verify(repository).save(cap.capture());
        Assertions.assertInstanceOf(Estudiante.class, cap.getValue());
        Assertions.assertEquals("hashed:password123", cap.getValue().getPassword());
        Assertions.assertTrue(cap.getValue().isActivo());
        Assertions.assertEquals("e@x.uy", cap.getValue().getEmail());
        Assertions.assertEquals(Rol.ESTUDIANTE, resp.rol());
    }

    @Test
    void crearProfesorSeteaTitulacionYCargo() {
        service.crear(crearReq(Rol.PROFESOR, "p@x.uy", null, null));
        ArgumentCaptor<Usuario> cap = ArgumentCaptor.forClass(Usuario.class);
        Mockito.verify(repository).save(cap.capture());
        Profesor p = Assertions.assertInstanceOf(Profesor.class, cap.getValue());
        Assertions.assertEquals("Doctorado", p.getTitulacion());
        Assertions.assertEquals("Titular", p.getCargo());
    }

    @Test
    void crearExternoSinInstitucionFalla() {
        Assertions.assertThrows(BusinessException.class,
                () -> service.crear(crearReq(Rol.EXTERNO, "x@x.uy", "  ", "Magíster")));
    }

    @Test
    void crearExternoOk() {
        service.crear(crearReq(Rol.EXTERNO, "x@x.uy", "UDELAR", "Magíster"));
        ArgumentCaptor<Usuario> cap = ArgumentCaptor.forClass(Usuario.class);
        Mockito.verify(repository).save(cap.capture());
        Externo e = Assertions.assertInstanceOf(Externo.class, cap.getValue());
        Assertions.assertEquals("UDELAR", e.getInstitucion());
    }

    @Test
    void crearConEmailExistenteFalla() {
        Mockito.when(repository.existsByEmail("dup@x.uy")).thenReturn(true);
        Assertions.assertThrows(BusinessException.class,
                () -> service.crear(crearReq(Rol.ESTUDIANTE, "dup@x.uy", null, null)));
    }

    @Test
    void crearConMatriculaExistenteFalla() {
        Mockito.when(repository.existsByMatricula(Mockito.any())).thenReturn(true);
        Assertions.assertThrows(BusinessException.class,
                () -> service.crear(crearReq(Rol.ESTUDIANTE, "nuevo@x.uy", null, null)));
    }

    @Test
    void crearSeteaMatricula() {
        service.crear(crearReq(Rol.PROFESOR, "m@x.uy", null, null));
        ArgumentCaptor<Usuario> cap = ArgumentCaptor.forClass(Usuario.class);
        Mockito.verify(repository).save(cap.capture());
        Assertions.assertEquals("MAT-m@x.uy", cap.getValue().getMatricula());
    }

    @Test
    void crearNormalizaEmailAMinusculas() {
        service.crear(crearReq(Rol.ESTUDIANTE, "  MixedCase@X.UY ", null, null));
        ArgumentCaptor<Usuario> cap = ArgumentCaptor.forClass(Usuario.class);
        Mockito.verify(repository).save(cap.capture());
        Assertions.assertEquals("mixedcase@x.uy", cap.getValue().getEmail());
    }

    @Test
    void actualizarCambiaDatosBasicos() {
        Estudiante u = new Estudiante();
        u.setId(1L);
        u.setEmail("old@x.uy");
        u.setNombre("Viejo");
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(u));
        service.actualizar(1L, new AdminUsuarioUpdateRequest("new@x.uy", "M-0001", "Nuevo", 40, "Salto", 8,
                null, null, null, null));
        Assertions.assertEquals("new@x.uy", u.getEmail());
        Assertions.assertEquals("Nuevo", u.getNombre());
        Assertions.assertEquals(8, u.getTopeAsignaciones());
    }

    @Test
    void desactivarPropiaCuentaFalla() {
        Administrador admin = new Administrador();
        admin.setId(7L);
        admin.setActivo(true);
        Mockito.when(repository.findById(7L)).thenReturn(Optional.of(admin));
        Assertions.assertThrows(BusinessException.class, () -> service.setActivo(7L, false, 7L));
    }

    @Test
    void desactivarUltimoAdminFalla() {
        Administrador admin = new Administrador();
        admin.setId(2L);
        admin.setActivo(true);
        Mockito.when(repository.findById(2L)).thenReturn(Optional.of(admin));
        Mockito.when(repository.contarAdministradoresActivos()).thenReturn(1L);
        Assertions.assertThrows(BusinessException.class, () -> service.setActivo(2L, false, 99L));
    }

    @Test
    void desactivarUsuarioNormalOk() {
        Estudiante u = new Estudiante();
        u.setId(3L);
        u.setActivo(true);
        Mockito.when(repository.findById(3L)).thenReturn(Optional.of(u));
        service.setActivo(3L, false, 99L);
        Assertions.assertFalse(u.isActivo());
    }

    @Test
    void buscarSinRolUsaBuscarAdmin() {
        Mockito.when(repository.buscarAdmin(Mockito.any(), Mockito.any())).thenReturn(Page.empty());
        service.buscar("ana", null, PageRequest.of(0, 10));
        Mockito.verify(repository).buscarAdmin(Mockito.eq("%ana%"), Mockito.any());
        Mockito.verify(repository, Mockito.never())
                .buscarAdminPorTipo(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void buscarConRolUsaBuscarAdminPorTipo() {
        Mockito.when(repository.buscarAdminPorTipo(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Page.empty());
        service.buscar("  ", Rol.PROFESOR, PageRequest.of(0, 10));
        Mockito.verify(repository).buscarAdminPorTipo(Mockito.isNull(), Mockito.eq(Profesor.class), Mockito.any());
    }

    @Test
    void resetPasswordHashea() {
        Estudiante u = new Estudiante();
        u.setId(5L);
        Mockito.when(repository.findById(5L)).thenReturn(Optional.of(u));
        service.resetPassword(5L, "nuevaClave1");
        Assertions.assertEquals("hashed:nuevaClave1", u.getPassword());
    }
}
