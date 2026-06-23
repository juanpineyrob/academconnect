package com.academconnect.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.academconnect.domain.Estudiante;
import com.academconnect.repository.MailPendienteRepository;
import com.academconnect.repository.UsuarioRepository;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AdminUsuarioOnboardingTests {

    private static final String ADMIN_EMAIL = "admin@academ.test";
    private static final String ESTU_EMAIL = "estu@academ.test";
    private static final String PASSWORD = "password123";

    @Autowired MockMvc mockMvc;
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

    @Test
    void altaManualCreaInvitadaSinPasswordYEncolaActivacion() throws Exception {
        mockMvc.perform(post("/admin/usuarios").cookie(login(ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rol":"ESTUDIANTE","email":"nuevo@academ.test","matricula":"2024999",\
                                "nombre":"Nuevo Alumno"}"""))
                .andExpect(status().isOk());

        var u = usuarioRepository.findByEmail("nuevo@academ.test").orElseThrow();
        assertThat(u.getEstadoCuenta()).isEqualTo(EstadoCuenta.INVITADA);
        assertThat(u.getPassword()).isNull();
        assertThat(u.isActivo()).isTrue();
        assertThat(mailRepo.findByEstadoOrderByCreatedAtAsc(EstadoMail.PENDIENTE, Pageable.ofSize(10))).hasSize(1);
    }

    @Test
    void enviarEnlacePasswordParaCuentaActivaDevuelve204YEncolaMail() throws Exception {
        var u = usuarioRepository.findByEmail(ESTU_EMAIL).orElseThrow();
        mockMvc.perform(post("/admin/usuarios/" + u.getId() + "/enviar-enlace-password").cookie(login(ADMIN_EMAIL)))
                .andExpect(status().isNoContent());

        assertThat(mailRepo.findByEstadoOrderByCreatedAtAsc(EstadoMail.PENDIENTE, Pageable.ofSize(10))).hasSize(1);
    }
}
