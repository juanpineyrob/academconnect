package com.academconnect.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.TestcontainersConfiguration;
import com.academconnect.config.CookieBearerTokenResolver;
import com.academconnect.domain.EstadoCuenta;
import com.academconnect.dto.EstudianteRequest;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.service.EstudianteService;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
public class AuthControllerTests {

    private static final String EMAIL = "auth-test@example.com";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EstudianteService estudianteService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void seedUsuario() {
        estudianteService.crear(new EstudianteRequest(EMAIL, PASSWORD, "Auth Tester", null, null, null));
    }

    @Test
    void loginShouldFailGenericallyWhenAccountIsInvitada() throws Exception {
        var u = usuarioRepository.findByEmail(EMAIL).orElseThrow();
        u.setEstadoCuenta(EstadoCuenta.INVITADA);
        u.setPassword(null);
        usuarioRepository.save(u);

        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(EMAIL, PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginShouldSetHttpOnlyCookieWhenCredentialsAreValid() throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(EMAIL, PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(CookieBearerTokenResolver.COOKIE_NAME))
                .andExpect(cookie().httpOnly(CookieBearerTokenResolver.COOKIE_NAME, true))
                .andExpect(cookie().path(CookieBearerTokenResolver.COOKIE_NAME, "/"))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("SameSite=Strict")));
    }

    @Test
    void loginShouldEmitSessionCookieWhenRememberIsFalse() throws Exception {
        String body = """
                {"email":"%s","password":"%s","remember":false}
                """.formatted(EMAIL, PASSWORD);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie(CookieBearerTokenResolver.COOKIE_NAME);
        org.junit.jupiter.api.Assertions.assertNotNull(cookie);
        org.junit.jupiter.api.Assertions.assertEquals(-1, cookie.getMaxAge(),
                "remember=false should emit a session cookie (Max-Age=-1)");
    }

    @Test
    void loginShouldEmitPersistentCookieWhenRememberIsTrue() throws Exception {
        String body = """
                {"email":"%s","password":"%s","remember":true}
                """.formatted(EMAIL, PASSWORD);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie(CookieBearerTokenResolver.COOKIE_NAME);
        org.junit.jupiter.api.Assertions.assertNotNull(cookie);
        org.junit.jupiter.api.Assertions.assertTrue(cookie.getMaxAge() > 0,
                "remember=true should emit a persistent cookie with positive Max-Age");
    }

    @Test
    void perfilShouldReturnOkWhenAuthenticatedViaCookie() throws Exception {
        Cookie jwtCookie = login();

        mockMvc.perform(get("/me/perfil").cookie(jwtCookie))
                .andExpect(status().isOk());
    }

    @Test
    void perfilShouldReturnUnauthorizedWhenNoCookieAndNoHeader() throws Exception {
        mockMvc.perform(get("/me/perfil"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutShouldClearCookieWithMaxAgeZero() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie(CookieBearerTokenResolver.COOKIE_NAME);
        org.junit.jupiter.api.Assertions.assertNotNull(cookie);
        org.junit.jupiter.api.Assertions.assertEquals(0, cookie.getMaxAge());
        org.junit.jupiter.api.Assertions.assertEquals("", cookie.getValue());
    }

    private Cookie login() throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(EMAIL, PASSWORD);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getCookie(CookieBearerTokenResolver.COOKIE_NAME);
    }
}
