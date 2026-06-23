package com.academconnect.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.PropositoToken;
import com.academconnect.domain.TokenCuenta;
import com.academconnect.domain.Usuario;
import com.academconnect.exception.TokenInvalidoException;
import com.academconnect.repository.TokenCuentaRepository;
import com.academconnect.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenCuentaService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final TokenCuentaRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${academconnect.onboarding.token-ttl-horas:48}")
    private long ttlHoras;

    /** Emite un token nuevo (invalidando los previos no usados del mismo propósito). Devuelve el token EN CLARO. */
    @Transactional
    public String emitir(Long usuarioId, PropositoToken proposito) {
        tokenRepository.deleteNoUsadosPorUsuarioYProposito(usuarioId, proposito);

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String claro = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        TokenCuenta t = new TokenCuenta();
        t.setUsuarioId(usuarioId);
        t.setTokenHash(hash(claro));
        t.setProposito(proposito);
        t.setExpiraEn(Instant.now().plus(ttlHoras, ChronoUnit.HOURS));
        tokenRepository.save(t);
        return claro;
    }

    /** Consume el token (un solo uso). Lanza BusinessException si es inválido/expirado/usado o de otro propósito. */
    @Transactional
    public Usuario consumir(String claro, PropositoToken propositoEsperado) {
        var token = tokenRepository.findByTokenHash(hash(claro))
                .filter(t -> t.getProposito() == propositoEsperado)
                .filter(t -> t.esUsable(Instant.now()))
                .orElseThrow(TokenInvalidoException::new);
        token.setUsadoEn(Instant.now());
        tokenRepository.save(token);
        return usuarioRepository.findById(token.getUsuarioId())
                .orElseThrow(TokenInvalidoException::new);
    }

    /** Verifica sin consumir. Devuelve el propósito si es usable, o null. */
    @Transactional(readOnly = true)
    public PropositoToken propositoSiUsable(String claro) {
        return tokenRepository.findByTokenHash(hash(claro))
                .filter(t -> t.esUsable(Instant.now()))
                .map(TokenCuenta::getProposito)
                .orElse(null);
    }

    private String hash(String claro) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(claro.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
