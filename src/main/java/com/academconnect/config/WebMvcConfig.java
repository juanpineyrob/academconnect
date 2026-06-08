package com.academconnect.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Expone los avatares como recurso estático bajo {@code /storage/avatars/**}.
 * Los PDFs de trabajos NO se sirven aquí: se entregan vía
 * {@code GET /api/trabajos/{id}/archivo}, que aplica reglas de acceso por estado y rol.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${academconnect.storage.avatars:./data/avatars}")
    private String avatarsRoot;

    @Value("${academconnect.storage.avatars-public-prefix:/storage/avatars}")
    private String avatarsPublicPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(avatarsPublicPrefix + "/**")
                .addResourceLocations(toLocation(avatarsRoot))
                .setCachePeriod(60);
    }

    private static String toLocation(String root) {
        return Path.of(root).toAbsolutePath().normalize().toUri().toString();
    }
}
