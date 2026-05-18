package com.academconnect.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.Documento;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    Optional<Documento> findBySha256(String sha256);

    Optional<Documento> findByStorageKey(String storageKey);
}
