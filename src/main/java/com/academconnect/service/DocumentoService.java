package com.academconnect.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.academconnect.domain.Documento;
import com.academconnect.exception.BusinessException;
import com.academconnect.repository.DocumentoRepository;
import com.academconnect.service.storage.DocumentoStorage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final DocumentoStorage documentoStorage;

    @Transactional
    public Documento guardarSiNuevo(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String sha256 = sha256Hex(bytes);

            return documentoRepository.findBySha256(sha256).orElseGet(() -> {
                try {
                    String storageKey = documentoStorage.store(
                            new ByteArrayInputStream(bytes),
                            file.getOriginalFilename(),
                            file.getContentType());

                    var doc = new Documento();
                    doc.setStorageKey(storageKey);
                    doc.setNombreOriginal(file.getOriginalFilename() != null ? file.getOriginalFilename() : "archivo");
                    doc.setMimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
                    doc.setSizeBytes(bytes.length);
                    doc.setSha256(sha256);
                    return documentoRepository.save(doc);
                } catch (IOException e) {
                    throw new BusinessException("Error al almacenar el archivo: " + e.getMessage());
                }
            });
        } catch (IOException e) {
            throw new BusinessException("Error al leer el archivo: " + e.getMessage());
        }
    }

    private String sha256Hex(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
