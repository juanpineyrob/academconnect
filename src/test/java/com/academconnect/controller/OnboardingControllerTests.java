package com.academconnect.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;

import com.academconnect.TestcontainersConfiguration;
import com.academconnect.domain.EstadoCuenta;
import com.academconnect.domain.EstadoMail;
import com.academconnect.domain.PropositoToken;
import com.academconnect.dto.EstudianteRequest;
import com.academconnect.repository.MailPendienteRepository;
import com.academconnect.repository.SolicitudCuentaRepository;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.service.EstudianteService;
import com.academconnect.service.TokenCuentaService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class OnboardingControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private TokenCuentaService tokenService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EstudianteService estudianteService;
    @Autowired private SolicitudCuentaRepository solicitudRepository;
    @Autowired private MailPendienteRepository mailRepo;

    private Long crearInvitada(String email) {
        var resp = estudianteService.crear(new EstudianteRequest(email, "x".repeat(8), "Inv", null, null, null));
        var u = usuarioRepository.findById(resp.id()).orElseThrow();
        u.setEstadoCuenta(EstadoCuenta.INVITADA);
        u.setPassword(null);
        usuarioRepository.save(u);
        return u.getId();
    }

    @Test
    void establecerPasswordActivaLaCuentaYPermiteLogin() throws Exception {
        Long id = crearInvitada("act@academ.test");
        String token = tokenService.emitir(id, PropositoToken.ACTIVACION);

        mockMvc.perform(post("/auth/password/establecer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"password\":\"NuevaPass123\"}"))
            .andExpect(status().isNoContent());

        var actualizado = usuarioRepository.findById(id).orElseThrow();
        assertThat(actualizado.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(actualizado.getPassword()).isNotNull();

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"act@academ.test\",\"password\":\"NuevaPass123\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void establecerPasswordConTokenInvalidoDevuelve400Generico() throws Exception {
        mockMvc.perform(post("/auth/password/establecer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"no-existe\",\"password\":\"NuevaPass123\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void verificarDevuelveTrueParaTokenUsable() throws Exception {
        Long id = crearInvitada("ver@academ.test");
        String token = tokenService.emitir(id, PropositoToken.ACTIVACION);

        mockMvc.perform(post("/auth/token/verificar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valido").value(true))
            .andExpect(jsonPath("$.proposito").value("ACTIVACION"));
    }

    @Test
    void verificarDevuelveFalseParaTokenInexistenteSinFiltrar() throws Exception {
        mockMvc.perform(post("/auth/token/verificar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"no-existe\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valido").value(false));
    }

    @Test
    void solicitarCuentaDevuelve202GenericoYPersiste() throws Exception {
        mockMvc.perform(post("/auth/solicitudes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"matricula\":\"2024001\",\"email\":\"nuevo@academ.test\",\"nombre\":\"Nuevo\"}"))
            .andExpect(status().isAccepted());
        assertThat(solicitudRepository.findAll()).anyMatch(s -> s.getMatricula().equals("2024001"));
    }

    @Test
    void recuperarSiempreDevuelve202AunqueElEmailNoExista() throws Exception {
        mockMvc.perform(post("/auth/password/recuperar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"fantasma@academ.test\"}"))
            .andExpect(status().isAccepted());
    }

    @Test
    void recuperarEncolaMailYTokenResetParaCuentaActiva() throws Exception {
        estudianteService.crear(new EstudianteRequest("activa2@academ.test", "x".repeat(8), "A", null, null, null));
        mockMvc.perform(post("/auth/password/recuperar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"activa2@academ.test\"}"))
            .andExpect(status().isAccepted());
        assertThat(mailRepo.findByEstadoOrderByCreatedAtAsc(EstadoMail.PENDIENTE, Pageable.ofSize(10))).hasSize(1);
    }

    @Test
    void reenviarActivacionEncolaMailParaCuentaInvitada() throws Exception {
        crearInvitada("inv-reenviar@academ.test");
        mockMvc.perform(post("/auth/activacion/reenviar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"inv-reenviar@academ.test\"}"))
            .andExpect(status().isAccepted());
        assertThat(mailRepo.findByEstadoOrderByCreatedAtAsc(EstadoMail.PENDIENTE, Pageable.ofSize(10))).hasSize(1);
    }
}
