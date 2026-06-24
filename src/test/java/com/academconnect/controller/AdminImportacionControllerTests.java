package com.academconnect.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.TestcontainersConfiguration;
import com.academconnect.config.CookieBearerTokenResolver;
import com.academconnect.domain.Administrador;
import com.academconnect.domain.EstadoCuenta;
import com.academconnect.domain.Estudiante;
import com.academconnect.repository.UsuarioRepository;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AdminImportacionControllerTests {

    private static final String ADMIN_EMAIL = "admin@academ.test";
    private static final String ESTU_EMAIL = "estu@academ.test";
    private static final String PASSWORD = "password123";

    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarioRepository;
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
    void previewYConfirmFlujoCompleto() throws Exception {
        var file = new MockMultipartFile("file", "padron.csv", "text/csv",
                "email,matricula,nombre\nz@academ.test,MZ,Z\n".getBytes());
        var res = mockMvc.perform(multipart("/admin/importaciones/preview").file(file).cookie(login(ADMIN_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nuevos").value(1))
                .andReturn().getResponse().getContentAsString();
        Long loteId = com.jayway.jsonpath.JsonPath.parse(res).read("$.loteId", Long.class);

        mockMvc.perform(post("/admin/importaciones/" + loteId + "/confirmar").cookie(login(ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reenviarInvitadas\":false}"))
                .andExpect(status().isNoContent());

        var creado = usuarioRepository.findByEmail("z@academ.test").orElseThrow();
        assertThat(creado.getEstadoCuenta()).isEqualTo(EstadoCuenta.INVITADA);
        assertThat(creado.getLoteImportacionId()).isEqualTo(loteId);
    }

    @Test
    void noAdminRecibe403() throws Exception {
        var file = new MockMultipartFile("file", "p.csv", "text/csv",
                "email,matricula,nombre\n".getBytes());
        mockMvc.perform(multipart("/admin/importaciones/preview").file(file).cookie(login(ESTU_EMAIL)))
                .andExpect(status().isForbidden());
    }
}
