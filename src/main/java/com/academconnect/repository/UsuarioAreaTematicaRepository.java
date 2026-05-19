package com.academconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.UsuarioAreaTematica;
import com.academconnect.domain.UsuarioAreaTematicaId;

public interface UsuarioAreaTematicaRepository extends JpaRepository<UsuarioAreaTematica, UsuarioAreaTematicaId> {

    List<UsuarioAreaTematica> findByIdUsuarioId(Long usuarioId);
}
