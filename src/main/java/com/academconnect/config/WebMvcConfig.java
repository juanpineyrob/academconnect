package com.academconnect.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Expone archivos estáticos del sistema (avatares de usuarios y PDFs de trabajos) bajo
 * prefijos públicos que coinciden con los valores persistidos en {@code Usuario.fotoUrl}
 * y {@code Trabajo.archivoUrl}.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${academconnect.storage.avatars:./data/avatars}")
    private String avatarsRoot;

    @Value("${academconnect.storage.avatars-public-prefix:/storage/avatars}")
    private String avatarsPublicPrefix;

    @Value("${academconnect.storage.trabajos:./data/trabajos}")
    private String trabajosRoot;

    @Value("${academconnect.storage.trabajos-public-prefix:/storage/trabajos}")
    private String trabajosPublicPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(avatarsPublicPrefix + "/**")
                .addResourceLocations(toLocation(avatarsRoot))
                .setCachePeriod(60);

        registry.addResourceHandler(trabajosPublicPrefix + "/**")
                .addResourceLocations(toLocation(trabajosRoot))
                .setCachePeriod(60);
    }

    private static String toLocation(String root) {
        return Path.of(root).toAbsolutePath().normalize().toUri().toString();
    }
}
