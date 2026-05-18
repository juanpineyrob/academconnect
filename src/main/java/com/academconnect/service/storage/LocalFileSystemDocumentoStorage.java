package com.academconnect.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.academconnect.exception.BusinessException;

@Component
public class LocalFileSystemDocumentoStorage implements DocumentoStorage {

    private static final DateTimeFormatter SUBDIR = DateTimeFormatter.ofPattern("yyyy/MM");

    private final Path root;

    public LocalFileSystemDocumentoStorage(
            @Value("${academconnect.storage.root:./data/documents}") String rootPath) throws IOException {
        this.root = Path.of(rootPath).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    @Override
    public String store(InputStream content, String nombreOriginal, String contentType) throws IOException {
        String extension = extractExtension(nombreOriginal);
        String storageKey = "%s/%s%s".formatted(
                LocalDate.now().format(SUBDIR),
                UUID.randomUUID(),
                extension);
        Path target = resolveSafe(storageKey);
        Files.createDirectories(target.getParent());
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        return storageKey;
    }

    @Override
    public InputStream retrieve(String storageKey) throws IOException {
        return Files.newInputStream(resolveSafe(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolveSafe(storageKey));
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.exists(resolveSafe(storageKey));
    }

    private Path resolveSafe(String storageKey) {
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new BusinessException("storage key inválida: " + storageKey);
        }
        return resolved;
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return idx > 0 ? filename.substring(idx).toLowerCase() : "";
    }
}
