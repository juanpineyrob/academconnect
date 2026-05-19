package com.academconnect.controller;

import com.academconnect.dto.AuthResponse;
import com.academconnect.dto.EstudianteRequest;
import com.academconnect.dto.ExternoRequest;
import com.academconnect.dto.LoginRequest;
import com.academconnect.dto.ProfesorRequest;
import com.academconnect.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
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
}
