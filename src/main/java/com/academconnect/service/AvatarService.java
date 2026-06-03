package com.academconnect.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.academconnect.dto.PerfilResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.UsuarioRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * Sube y almacena la foto de perfil del usuario autenticado. Valida tipo/tamaño,
 * persiste en filesystem bajo {@code academconnect.storage.avatars} y actualiza
 * {@code Usuario.fotoUrl} con una ruta relativa pública.
 */
@Service
@RequiredArgsConstructor
public class AvatarService {

    private static final Map<String, String> EXT_BY_MIME = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private static final Set<String> ALLOWED_MIMES = EXT_BY_MIME.keySet();

    private final UsuarioRepository usuarioRepository;
    private final PerfilService perfilService;

    @Value("${academconnect.storage.avatars:./data/avatars}")
    private String avatarsRootPath;

    @Value("${academconnect.storage.avatars-public-prefix:/storage/avatars}")
    private String publicPrefix;

    @Value("${academconnect.storage.avatars-max-bytes:2097152}")
    private long maxBytes;

    private Path avatarsRoot;

    @PostConstruct
    void init() throws IOException {
        this.avatarsRoot = Path.of(avatarsRootPath).toAbsolutePath().normalize();
        Files.createDirectories(this.avatarsRoot);
    }

    @Transactional
    public PerfilResponse subirAvatar(String email, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Archivo vacío o no enviado.");
        }
        String mime = file.getContentType();
        if (mime == null || !ALLOWED_MIMES.contains(mime.toLowerCase())) {
            throw new BusinessException(
                    "Tipo de archivo no permitido. Aceptados: JPG, PNG, WEBP.");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException(
                    "El archivo supera el tamaño máximo permitido (%d MB)."
                            .formatted(maxBytes / (1024 * 1024)));
        }

        var usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email));

        String ext = EXT_BY_MIME.get(mime.toLowerCase());
        String filename = "%d%s".formatted(usuario.getId(), ext);
        Path target = avatarsRoot.resolve(filename).normalize();
        if (!target.startsWith(avatarsRoot)) {
            throw new BusinessException("Ruta de avatar inválida.");
        }

        try {
            deleteExistingAvatars(usuario.getId());
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException("Error al almacenar el avatar: " + e.getMessage());
        }

        usuario.setFotoUrl(publicPrefix + "/" + filename);
        usuarioRepository.save(usuario);
        return perfilService.obtenerPerfil(email);
    }

    private void deleteExistingAvatars(Long userId) throws IOException {
        String prefix = userId + ".";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(avatarsRoot, p -> p.getFileName().toString().startsWith(prefix))) {
            for (Path p : stream) {
                Files.deleteIfExists(p);
            }
        }
    }
}
