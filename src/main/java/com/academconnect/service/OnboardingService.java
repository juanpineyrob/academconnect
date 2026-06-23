package com.academconnect.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.EstadoCuenta;
import com.academconnect.domain.PropositoToken;
import com.academconnect.domain.Usuario;
import com.academconnect.exception.TokenInvalidoException;
import com.academconnect.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final TokenCuentaService tokenService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /** Consume el token (ACTIVACION o RESET) y fija la contraseña; activa la cuenta si era ACTIVACION. */
    @Transactional
    public void establecerPassword(String tokenClaro, String passwordPlano) {
        PropositoToken proposito = tokenService.propositoSiUsable(tokenClaro);
        if (proposito == null) {
            throw new TokenInvalidoException();
        }
        Usuario u = tokenService.consumir(tokenClaro, proposito);
        u.setPassword(passwordEncoder.encode(passwordPlano));
        if (proposito == PropositoToken.ACTIVACION) {
            u.setEstadoCuenta(EstadoCuenta.ACTIVA);
        }
        usuarioRepository.save(u);
    }
}
