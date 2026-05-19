package com.academconnect.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.Externo;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.Usuario;
import com.academconnect.domain.UsuarioAreaTematica;
import com.academconnect.dto.PerfilResponse;
import com.academconnect.dto.PerfilUpdateRequest;
import com.academconnect.dto.UsuarioAreaTematicaResponse;
import com.academconnect.dto.UsuarioAreasRequest;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.AreaTematicaRepository;
import com.academconnect.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final AreaTematicaRepository areaTematicaRepository;
    private final PasswordEncoder passwordEncoder;

    public PerfilResponse obtenerPerfil(String email) {
        var usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email));
        return toPerfilResponse(usuario);
    }

    @Transactional
    public PerfilResponse actualizarPerfil(String email, PerfilUpdateRequest request) {
        var usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email));

        usuario.setNombre(request.nombre());
        usuario.setEdad(request.edad());
        usuario.setUbicacion(request.ubicacion());
        usuario.setBiografia(request.biografia());

        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.password()));
        }

        if (usuario instanceof Profesor p) {
            if (request.titulacion() != null) p.setTitulacion(request.titulacion());
            if (request.cargo() != null) p.setCargo(request.cargo());
        } else if (usuario instanceof Externo e) {
            if (request.institucion() != null) e.setInstitucion(request.institucion());
            if (request.titulo() != null) e.setTitulo(request.titulo());
        }

        return toPerfilResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public List<UsuarioAreaTematicaResponse> actualizarAreas(String email, UsuarioAreasRequest request) {
        var usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email));

        usuario.getAreas().clear();

        request.areas().forEach(item -> {
            var area = areaTematicaRepository.findById(item.areaId())
                    .orElseThrow(() -> new ResourceNotFoundException("AreaTematica", item.areaId()));
            usuario.getAreas().add(new UsuarioAreaTematica(usuario, area, item.nivelExperticia()));
        });

        usuarioRepository.save(usuario);
        return toAreaResponses(usuario.getAreas());
    }

    private PerfilResponse toPerfilResponse(Usuario u) {
        return new PerfilResponse(
                u.getId(),
                u.getEmail(),
                u.getNombre(),
                u.isActivo(),
                u.getRol(),
                u.getEdad(),
                u.getUbicacion(),
                u.getBiografia(),
                u instanceof Profesor p ? p.getTitulacion() : null,
                u instanceof Profesor p2 ? p2.getCargo() : null,
                u instanceof Externo e ? e.getInstitucion() : null,
                u instanceof Externo e2 ? e2.getTitulo() : null,
                toAreaResponseSet(u.getAreas()),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }

    private Set<UsuarioAreaTematicaResponse> toAreaResponseSet(Set<UsuarioAreaTematica> areas) {
        return areas.stream()
                .map(uat -> new UsuarioAreaTematicaResponse(
                        uat.getArea().getId(),
                        uat.getArea().getNombre(),
                        uat.getNivelExperticia()))
                .collect(Collectors.toSet());
    }

    private List<UsuarioAreaTematicaResponse> toAreaResponses(Set<UsuarioAreaTematica> areas) {
        return areas.stream()
                .map(uat -> new UsuarioAreaTematicaResponse(
                        uat.getArea().getId(),
                        uat.getArea().getNombre(),
                        uat.getNivelExperticia()))
                .toList();
    }
}
