package com.academconnect.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.domain.PropositoToken;
import com.academconnect.dto.EstablecerPasswordRequest;
import com.academconnect.dto.VerificarTokenRequest;
import com.academconnect.dto.VerificarTokenResponse;
import com.academconnect.service.OnboardingService;
import com.academconnect.service.TokenCuentaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final TokenCuentaService tokenService;

    @PostMapping("/password/establecer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void establecerPassword(@Valid @RequestBody EstablecerPasswordRequest request) {
        onboardingService.establecerPassword(request.token(), request.password());
    }

    @PostMapping("/token/verificar")
    public VerificarTokenResponse verificar(@Valid @RequestBody VerificarTokenRequest request) {
        PropositoToken proposito = tokenService.propositoSiUsable(request.token());
        return new VerificarTokenResponse(proposito != null, proposito);
    }
}
