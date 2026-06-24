package com.academconnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;

import com.academconnect.TestcontainersConfiguration;
import com.academconnect.domain.Administrador;
import com.academconnect.domain.EstadoCuenta;
import com.academconnect.domain.EstadoMail;
import com.academconnect.dto.EstudianteRequest;
import com.academconnect.dto.ImportConfirmRequest;
import com.academconnect.exception.BusinessException;
import com.academconnect.repository.MailPendienteRepository;
import com.academconnect.repository.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ImportacionUsuariosServiceTests {

    @Autowired ImportacionUsuariosService service;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired EstudianteService estudianteService;
    @Autowired MailPendienteRepository mailRepo;

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

    @Test
    void confirmarCreaSoloLosNuevosComoInvitadosYEncolaUnMailCadaUno() {
        Long admin = adminId();
        String csv = String.join("\n", "email,matricula,nombre",
                "a@academ.test,MA,A", "b@academ.test,MB,B");
        var preview = service.preview("p.csv", csv.getBytes(StandardCharsets.UTF_8), admin);

        long mailsAntes = mailRepo.findByEstadoOrderByCreatedAtAsc(EstadoMail.PENDIENTE, Pageable.ofSize(100)).size();
        service.confirmar(preview.loteId(), new ImportConfirmRequest(false), admin);

        var a = usuarioRepository.findByEmail("a@academ.test").orElseThrow();
        assertThat(a.getEstadoCuenta()).isEqualTo(EstadoCuenta.INVITADA);
        assertThat(a.getPassword()).isNull();
        assertThat(a.getLoteImportacionId()).isEqualTo(preview.loteId());
        long mailsDespues = mailRepo.findByEstadoOrderByCreatedAtAsc(EstadoMail.PENDIENTE, Pageable.ofSize(100)).size();
        assertThat(mailsDespues - mailsAntes).isEqualTo(2);
    }

    @Test
    void confirmarEsIdempotenteNoRecreaNiPisaExistentes() {
        Long admin = adminId();
        String csv = String.join("\n", "email,matricula,nombre", "c@academ.test,MC,C");
        var p1 = service.preview("p.csv", csv.getBytes(StandardCharsets.UTF_8), admin);
        service.confirmar(p1.loteId(), new ImportConfirmRequest(false), admin);
        long antes = usuarioRepository.count();

        var p2 = service.preview("p.csv", csv.getBytes(StandardCharsets.UTF_8), admin);
        // tras el commit previo, la fila ahora clasifica EXISTE_INVITADA
        assertThat(p2.items().get(0).resultado())
                .isEqualTo(com.academconnect.domain.ResultadoFila.EXISTE_INVITADA);
        service.confirmar(p2.loteId(), new ImportConfirmRequest(false), admin); // skip -> no crea nada
        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    @Test
    void confirmarUnLoteYaConfirmadoLanza() {
        Long admin = adminId();
        String csv = "email,matricula,nombre\nd@academ.test,MD,D\n";
        var p = service.preview("p.csv", csv.getBytes(StandardCharsets.UTF_8), admin);
        service.confirmar(p.loteId(), new ImportConfirmRequest(false), admin);
        org.junit.jupiter.api.Assertions.assertThrows(BusinessException.class,
                () -> service.confirmar(p.loteId(), new ImportConfirmRequest(false), admin));
    }

    @Test
    void confirmarReenviaInvitadasSoloSiSeSolicita() {
        Long admin = adminId();
        String csv = "email,matricula,nombre\nre@academ.test,MRE,Re\n";
        var p1 = service.preview("p.csv", csv.getBytes(StandardCharsets.UTF_8), admin);
        service.confirmar(p1.loteId(), new ImportConfirmRequest(false), admin);

        var p2 = service.preview("p.csv", csv.getBytes(StandardCharsets.UTF_8), admin);
        long mailsAntes = mailRepo.findByEstadoOrderByCreatedAtAsc(EstadoMail.PENDIENTE, Pageable.ofSize(100)).size();
        service.confirmar(p2.loteId(), new ImportConfirmRequest(true), admin); // reenviar invitadas
        long mailsDespues = mailRepo.findByEstadoOrderByCreatedAtAsc(EstadoMail.PENDIENTE, Pageable.ofSize(100)).size();
        assertThat(mailsDespues - mailsAntes).isEqualTo(1);
    }
}
