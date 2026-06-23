package com.academconnect.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class MailTemplateService {

    public record MailContenido(String asunto, String html, String texto) {
    }

    @Value("${academconnect.frontend.base-url}")
    private String frontBase;

    @Value("${academconnect.onboarding.token-ttl-horas:48}")
    private long ttlHoras;

    public MailContenido activacion(String nombre, String tokenClaro) {
        Map<String, String> vars = vars(nombre, tokenClaro);
        return new MailContenido("Activá tu cuenta de AcademConnect",
                render("mail/activacion.html", vars), render("mail/activacion.txt", vars));
    }

    public MailContenido restablecer(String nombre, String tokenClaro) {
        Map<String, String> vars = vars(nombre, tokenClaro);
        return new MailContenido("Restablecé tu contraseña de AcademConnect",
                render("mail/restablecer.html", vars), render("mail/restablecer.txt", vars));
    }

    private Map<String, String> vars(String nombre, String token) {
        return Map.of(
                "nombre", nombre == null ? "" : nombre,
                "enlace", frontBase + "/establecer-password?token=" + token,
                "ttlHoras", String.valueOf(ttlHoras));
    }

    private String render(String path, Map<String, String> vars) {
        String tpl;
        try {
            tpl = new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer plantilla " + path, e);
        }
        for (var e : vars.entrySet()) {
            tpl = tpl.replace("{{" + e.getKey() + "}}", e.getValue());
        }
        return tpl;
    }
}
