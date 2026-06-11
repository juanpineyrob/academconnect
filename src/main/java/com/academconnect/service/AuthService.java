package com.academconnect.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.Usuario;
import com.academconnect.dto.AuthResponse;
import com.academconnect.dto.EstudianteRequest;
import com.academconnect.dto.ExternoRequest;
import com.academconnect.dto.LoginRequest;
import com.academconnect.dto.ProfesorRequest;
import com.academconnect.exception.BusinessException;
import com.academconnect.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final EstudianteService estudianteService;
    private final ProfesorService profesorService;
    private final ExternoService externoService;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @Value("${academconnect.security.jwt.expiration-seconds:86400}")
    private long expirationSeconds;

    public AuthResponse login(LoginRequest request) {
        var usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        if (!usuario.isActivo()) {
            throw new BusinessException("Cuenta desactivada. Contacte al administrador.");
        }
        return buildResponse(usuario);
    }

    @Transactional
    public AuthResponse registerEstudiante(EstudianteRequest request) {
        var resp = estudianteService.crear(request);
        var usuario = usuarioRepository.findById(resp.id()).orElseThrow();
        return buildResponse(usuario);
    }

    @Transactional
    public AuthResponse registerProfesor(ProfesorRequest request) {
        var resp = profesorService.crear(request);
        var usuario = usuarioRepository.findById(resp.id()).orElseThrow();
        return buildResponse(usuario);
    }

    @Transactional
    public AuthResponse registerExterno(ExternoRequest request) {
        var resp = externoService.crear(request);
        var usuario = usuarioRepository.findById(resp.id()).orElseThrow();
        return buildResponse(usuario);
    }

    private AuthResponse buildResponse(Usuario usuario) {
        return new AuthResponse(generateToken(usuario), usuario.getId(), usuario.getNombre(),
                usuario.getEmail(), usuario.getRol(), usuario.getFotoUrl());
    }

    private String generateToken(Usuario usuario) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("academconnect")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .subject(usuario.getEmail())
                .claim("userId", usuario.getId())
                .claim("rol", usuario.getRol().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
