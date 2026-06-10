package com.academconnect.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.TestcontainersConfiguration;
import com.academconnect.dto.EstudianteRequest;
import com.academconnect.dto.EstudianteResponse;
import com.academconnect.service.EstudianteService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
public class UsuarioControllerTests {

    private static final String EMAIL = "perfil-publico-test@example.com";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EstudianteService estudianteService;

    private Long knownActiveUserId;

    @BeforeEach
    void seedUsuario() {
        EstudianteResponse response = estudianteService.crear(
                new EstudianteRequest(EMAIL, PASSWORD, "Perfil Tester", null, null, null));
        knownActiveUserId = response.id();
    }

    @Test
    void perfilPublico_anonimo_recibe_200_sin_email() throws Exception {
        mockMvc.perform(get("/api/usuarios/{id}/perfil", knownActiveUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.nombre").exists())
                .andExpect(jsonPath("$.id").value(knownActiveUserId));
    }

    @Test
    void perfilPublico_usuario_inexistente_devuelve_404() throws Exception {
        mockMvc.perform(get("/api/usuarios/{id}/perfil", 999999L))
                .andExpect(status().isNotFound());
    }
}
