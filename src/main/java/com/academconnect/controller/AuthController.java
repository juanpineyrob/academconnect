package com.academconnect.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.academconnect.config.CookieBearerTokenResolver;
import com.academconnect.dto.AuthResponse;
import com.academconnect.dto.EstudianteRequest;
import com.academconnect.dto.ExternoRequest;
import com.academconnect.dto.LoginRequest;
import com.academconnect.dto.ProfesorRequest;
import com.academconnect.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${academconnect.security.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${academconnect.security.cookie.same-site:Strict}")
    private String cookieSameSite;

    @Value("${academconnect.security.cookie.remember-days:14}")
    private long rememberDays;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        long maxAge = request.rememberOrDefault() ? Duration.ofDays(rememberDays).toSeconds() : -1;
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildJwtCookie(response.token(), maxAge).toString())
                .body(response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, buildJwtCookie("", 0).toString())
                .build();
    }

    @PostMapping("/register/estudiante")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerEstudiante(@Valid @RequestBody EstudianteRequest request) {
        return authService.registerEstudiante(request);
    }

    @PostMapping("/register/profesor")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerProfesor(@Valid @RequestBody ProfesorRequest request) {
        return authService.registerProfesor(request);
    }

    @PostMapping("/register/externo")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerExterno(@Valid @RequestBody ExternoRequest request) {
        return authService.registerExterno(request);
    }

    private ResponseCookie buildJwtCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(CookieBearerTokenResolver.COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
