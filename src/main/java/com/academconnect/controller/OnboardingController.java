package com.academconnect.controller;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.domain.PropositoToken;
import com.academconnect.dto.EmailRequest;
import com.academconnect.dto.EstablecerPasswordRequest;
import com.academconnect.dto.SolicitudCuentaRequest;
import com.academconnect.dto.VerificarTokenRequest;
import com.academconnect.dto.VerificarTokenResponse;
import com.academconnect.exception.RateLimitException;
import com.academconnect.service.OnboardingService;
import com.academconnect.service.RateLimiterService;
import com.academconnect.service.TokenCuentaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class OnboardingController {

    private static final String MENSAJE_GENERICO = "Si corresponde, enviaremos un enlace al correo indicado.";

    private final OnboardingService onboardingService;
    private final TokenCuentaService tokenService;
    private final RateLimiterService rateLimiter;

    @PostMapping("/password/establecer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void establecerPassword(@Valid @RequestBody EstablecerPasswordRequest request,
                                   HttpServletRequest http) {
        if (!rateLimiter.permitir("establecer:" + http.getRemoteAddr(), 10, Duration.ofHours(1))) {
            throw new RateLimitException();
        }
        onboardingService.establecerPassword(request.token(), request.password());
    }

    @PostMapping("/token/verificar")
    public VerificarTokenResponse verificar(@Valid @RequestBody VerificarTokenRequest request,
                                            HttpServletRequest http) {
        if (!rateLimiter.permitir("verificar:" + http.getRemoteAddr(), 30, Duration.ofHours(1))) {
            throw new RateLimitException();
        }
        PropositoToken proposito = tokenService.propositoSiUsable(request.token());
        return new VerificarTokenResponse(proposito != null, proposito);
    }

    @PostMapping("/solicitudes")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> solicitar(@Valid @RequestBody SolicitudCuentaRequest req,
                                         HttpServletRequest http) {
        if (!rateLimiter.permitir("solicitud:" + http.getRemoteAddr(), 5, Duration.ofHours(1))) {
            throw new RateLimitException();
        }
        onboardingService.crearSolicitud(req.matricula(), req.email(), req.nombre());
        return Map.of("mensaje", MENSAJE_GENERICO);
    }

    @PostMapping("/password/recuperar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> recuperar(@Valid @RequestBody EmailRequest req, HttpServletRequest http) {
        if (!rateLimiter.permitir("recuperar:" + req.email() + ":" + http.getRemoteAddr(), 5, Duration.ofHours(1))) {
            throw new RateLimitException();
        }
        onboardingService.solicitarReset(req.email());
        return Map.of("mensaje", MENSAJE_GENERICO);
    }

    @PostMapping("/activacion/reenviar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> reenviar(@Valid @RequestBody EmailRequest req, HttpServletRequest http) {
        if (!rateLimiter.permitir("reenviar:" + req.email() + ":" + http.getRemoteAddr(), 5, Duration.ofHours(1))) {
            throw new RateLimitException();
        }
        onboardingService.reenviarActivacion(req.email());
        return Map.of("mensaje", MENSAJE_GENERICO);
    }
}
