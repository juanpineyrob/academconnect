package com.academconnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.TestcontainersConfiguration;
import com.academconnect.domain.Administrador;
import com.academconnect.domain.EstadoCuenta;
import com.academconnect.dto.EstudianteRequest;
import com.academconnect.repository.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ImportacionUsuariosServiceTests {

    @Autowired ImportacionUsuariosService service;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired EstudianteService estudianteService;

    private Long adminId() {
        return usuarioRepository.findByEmail("admin-import@academ.test").orElseGet(() -> {
            Administrador a = new Administrador();
            a.setEmail("admin-import@academ.test");
            a.setMatricula("ADM-IMP");
            a.setNombre("Admin Import");
            a.setActivo(true);
            a.setEstadoCuenta(EstadoCuenta.ACTIVA);
            a.setPassword("$2a$10$abcdefghijklmnopqrstuv");
            return usuarioRepository.save(a);
        }).getId();
    }

    private void crearActivaConMatricula(String email, String matricula) {
        var resp = estudianteService.crear(new EstudianteRequest(email, "x".repeat(8), "Sembrado", null, null, null));
        var u = usuarioRepository.findById(resp.id()).orElseThrow();
        u.setMatricula(matricula);
        usuarioRepository.save(u);
    }

    @Test
    void previewClasificaNuevosExistentesYColisiones() {
        crearActivaConMatricula("e1@academ.test", "M-EXISTE");

        String csv = String.join("\n",
                "email,matricula,nombre",
                "nuevo@academ.test,M-NUEVO,Nuevo",   // NUEVO
                "e1@academ.test,M-EXISTE,E1",        // EXISTE_ACTIVA (par exacto)
                "otro@academ.test,M-EXISTE,Otro",    // COLISION_MATRICULA (matrícula con email distinto)
                "e1@academ.test,M-OTRA,EE",          // COLISION_EMAIL (email usado por otra matrícula)
                "malformada-sin-campos");            // ERROR_FORMATO

        var preview = service.preview("padron.csv", csv.getBytes(StandardCharsets.UTF_8), adminId());

        assertThat(preview.total()).isEqualTo(5);
        assertThat(preview.nuevos()).isEqualTo(1);
        assertThat(preview.existentes()).isEqualTo(1);
        assertThat(preview.errores()).isEqualTo(3); // 2 colisiones + 1 formato
        assertThat(preview.loteId()).isNotNull();
        assertThat(preview.items()).hasSize(5);
    }

    @Test
    void previewNoCreaUsuarios() {
        Long admin = adminId();
        long antes = usuarioRepository.count();
        String csv = "email,matricula,nombre\nnuevo@academ.test,M-NUEVO,Nuevo\n";
        service.preview("p.csv", csv.getBytes(StandardCharsets.UTF_8), admin);
        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    @Test
    void previewClasificaCamposVaciosComoErrorFormato() {
        String csv = "email,matricula,nombre\n,,\n";
        var preview = service.preview("p.csv", csv.getBytes(StandardCharsets.UTF_8), adminId());
        assertThat(preview.errores()).isEqualTo(1);
        assertThat(preview.nuevos()).isZero();
    }

    @Test
    void existeInvitadaSeClasificaDistintoDeActiva() {
        var resp = estudianteService.crear(new EstudianteRequest("inv@academ.test", "x".repeat(8), "Inv", null, null, null));
        var u = usuarioRepository.findById(resp.id()).orElseThrow();
        u.setMatricula("M-INV");
        u.setEstadoCuenta(EstadoCuenta.INVITADA);
        u.setPassword(null);
        usuarioRepository.save(u);

        String csv = "email,matricula,nombre\ninv@academ.test,M-INV,Inv\n";
        var preview = service.preview("p.csv", csv.getBytes(StandardCharsets.UTF_8), adminId());

        assertThat(preview.existentes()).isEqualTo(1);
        assertThat(preview.items().get(0).resultado())
                .isEqualTo(com.academconnect.domain.ResultadoFila.EXISTE_INVITADA);
    }
}
