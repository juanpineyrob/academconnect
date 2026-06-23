package com.academconnect.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.TestcontainersConfiguration;
import com.academconnect.config.CookieBearerTokenResolver;
import com.academconnect.domain.Administrador;
import com.academconnect.domain.EstadoCuenta;
import com.academconnect.domain.EstadoMail;
import com.academconnect.domain.EstadoSolicitudCuenta;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.SolicitudCuenta;
import com.academconnect.repository.MailPendienteRepository;
import com.academconnect.repository.SolicitudCuentaRepository;
import com.academconnect.repository.UsuarioRepository;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AdminSolicitudControllerTests {

    private static final String ADMIN_EMAIL = "admin@academ.test";
    private static final String ESTU_EMAIL = "estu@academ.test";
    private static final String PASSWORD = "password123";

    @Autowired MockMvc mockMvc;
    @Autowired SolicitudCuentaRepository repo;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired MailPendienteRepository mailRepo;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUsuarios() {
        if (usuarioRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {
            Administrador a = new Administrador();
            a.setEmail(ADMIN_EMAIL);
            a.setMatricula("ADM-1");
            a.setNombre("Admin");
            a.setActivo(true);
            a.setEstadoCuenta(EstadoCuenta.ACTIVA);
            a.setPassword(passwordEncoder.encode(PASSWORD));
            usuarioRepository.save(a);
        }
        if (usuarioRepository.findByEmail(ESTU_EMAIL).isEmpty()) {
            Estudiante e = new Estudiante();
            e.setEmail(ESTU_EMAIL);
            e.setMatricula("EST-1");
            e.setNombre("Estu");
            e.setActivo(true);
            e.setEstadoCuenta(EstadoCuenta.ACTIVA);
            e.setPassword(passwordEncoder.encode(PASSWORD));
            usuarioRepository.save(e);
        }
    }

    private Cookie login(String email) throws Exception {
        var result = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie(CookieBearerTokenResolver.COOKIE_NAME);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private Long seedSolicitud() {
        var s = new SolicitudCuenta();
        s.setMatricula("2024777");
        s.setEmail("ped@academ.test");
        s.setNombre("Ped");
        return repo.save(s).getId();
    }

    @Test
    void aprobarCreaCuentaInvitadaEstudiante() throws Exception {
        Long id = seedSolicitud();
        mockMvc.perform(post("/admin/solicitudes/" + id + "/aprobar").cookie(login(ADMIN_EMAIL)))
                .andExpect(status().isOk());
        var u = usuarioRepository.findByEmail("ped@academ.test").orElseThrow();
        assertThat(u.getEstadoCuenta()).isEqualTo(EstadoCuenta.INVITADA);
        assertThat(u.getPassword()).isNull();
        assertThat(u.getMatricula()).isEqualTo("2024777");
        assertThat(repo.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoSolicitudCuenta.APROBADA);
        assertThat(mailRepo.findByEstadoOrderByCreatedAtAsc(EstadoMail.PENDIENTE, Pageable.ofSize(10))).hasSize(1);
    }

    @Test
    void aprobarConEmailOMatriculaExistenteDevuelve409() throws Exception {
        var s = new SolicitudCuenta();
        s.setMatricula("EST-1"); // matrícula ya usada por el estudiante sembrado
        s.setEmail(ESTU_EMAIL); // email ya usado por el estudiante sembrado
        s.setNombre("Ped");
        Long id = repo.save(s).getId();
        mockMvc.perform(post("/admin/solicitudes/" + id + "/aprobar").cookie(login(ADMIN_EMAIL)))
                .andExpect(status().isConflict());
        assertThat(repo.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoSolicitudCuenta.PENDIENTE);
    }

    @Test
    void aprobarSolicitudYaResueltaDevuelve409() throws Exception {
        var s = new SolicitudCuenta();
        s.setMatricula("2024888");
        s.setEmail("rej@academ.test");
        s.setNombre("Rej");
        s.setEstado(EstadoSolicitudCuenta.RECHAZADA);
        Long id = repo.save(s).getId();
        mockMvc.perform(post("/admin/solicitudes/" + id + "/aprobar").cookie(login(ADMIN_EMAIL)))
                .andExpect(status().isConflict());
        assertThat(repo.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoSolicitudCuenta.RECHAZADA);
    }

    @Test
    void noAdminRecibe403() throws Exception {
        mockMvc.perform(get("/admin/solicitudes").cookie(login(ESTU_EMAIL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazarGuardaMotivo() throws Exception {
        Long id = seedSolicitud();
        mockMvc.perform(post("/admin/solicitudes/" + id + "/rechazar").cookie(login(ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"matrícula no coincide\"}"))
                .andExpect(status().isOk());
        assertThat(repo.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoSolicitudCuenta.RECHAZADA);
        assertThat(repo.findById(id).orElseThrow().getMotivoRechazo()).isEqualTo("matrícula no coincide");
    }

    @Test
    void listarFiltraPorTextoConDatosReales() throws Exception {
        seedSolicitud();
        mockMvc.perform(get("/admin/solicitudes").param("q", "PED").cookie(login(ADMIN_EMAIL)))
                .andExpect(status().isOk());
        assertThat(repo.buscar(null, "%ped%", Pageable.ofSize(10)).getTotalElements()).isEqualTo(1);
        assertThat(repo.buscar(EstadoSolicitudCuenta.PENDIENTE, "%2024777%", Pageable.ofSize(10))
                .getTotalElements()).isEqualTo(1);
    }
}
