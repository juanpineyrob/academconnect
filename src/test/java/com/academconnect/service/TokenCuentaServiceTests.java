package com.academconnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.TestcontainersConfiguration;
import com.academconnect.domain.EstadoCuenta;
import com.academconnect.domain.PropositoToken;
import com.academconnect.dto.EstudianteRequest;
import com.academconnect.exception.BusinessException;
import com.academconnect.repository.TokenCuentaRepository;
import com.academconnect.repository.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class TokenCuentaServiceTests {

    @Autowired private TokenCuentaService tokenService;
    @Autowired private TokenCuentaRepository tokenRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EstudianteService estudianteService;

    private Long crearInvitada() {
        var resp = estudianteService.crear(new EstudianteRequest(
                "tok@academ.test", "Password123", "Tok", null, null, null));
        var u = usuarioRepository.findById(resp.id()).orElseThrow();
        u.setEstadoCuenta(EstadoCuenta.INVITADA);
        u.setPassword(null);
        usuarioRepository.save(u);
        return u.getId();
    }

    @Test
    void emitirGuardaSoloHashYDevuelveTokenEnClaro() {
        Long id = crearInvitada();
        String claro = tokenService.emitir(id, PropositoToken.ACTIVACION);

        assertThat(claro).isNotBlank().hasSizeGreaterThanOrEqualTo(32);
        var guardado = tokenRepository.findAll().get(0);
        assertThat(guardado.getTokenHash()).isNotEqualTo(claro);
        assertThat(guardado.getTokenHash()).hasSize(64);
    }

    @Test
    void emitirInvalidaTokenPrevioDelMismoProposito() {
        Long id = crearInvitada();
        tokenService.emitir(id, PropositoToken.ACTIVACION);
        tokenService.emitir(id, PropositoToken.ACTIVACION);

        assertThat(tokenRepository.findAll()).hasSize(1);
    }

    @Test
    void consumirDevuelveUsuarioYMarcaUsadoUnaSolaVez() {
        Long id = crearInvitada();
        String claro = tokenService.emitir(id, PropositoToken.ACTIVACION);

        var consumido = tokenService.consumir(claro, PropositoToken.ACTIVACION);
        assertThat(consumido.getId()).isEqualTo(id);

        org.junit.jupiter.api.Assertions.assertThrows(BusinessException.class,
                () -> tokenService.consumir(claro, PropositoToken.ACTIVACION));
    }

    @Test
    void propositoSiUsableDevuelveNullParaTokenInexistente() {
        assertThat(tokenService.propositoSiUsable("no-existe")).isNull();
    }
}
