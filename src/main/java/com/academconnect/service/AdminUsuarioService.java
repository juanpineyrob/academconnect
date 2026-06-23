package com.academconnect.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.Administrador;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.Externo;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.Rol;
import com.academconnect.domain.Usuario;
import com.academconnect.dto.AdminUsuarioCreateRequest;
import com.academconnect.dto.AdminUsuarioResponse;
import com.academconnect.dto.AdminUsuarioUpdateRequest;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminUsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public Page<AdminUsuarioResponse> buscar(String q, Rol rol, Pageable pageable) {
        String qn = (q == null || q.isBlank()) ? null : q.trim();
        Page<Usuario> page = (rol == null)
                ? repository.buscarAdmin(qn, pageable)
                : repository.buscarAdminPorTipo(qn, claseDe(rol), pageable);
        return page.map(this::toResponse);
    }

    private static Class<? extends Usuario> claseDe(Rol rol) {
        return switch (rol) {
            case ESTUDIANTE -> Estudiante.class;
            case PROFESOR -> Profesor.class;
            case EXTERNO -> Externo.class;
            case ADMINISTRADOR -> Administrador.class;
        };
    }

    @Transactional
    public AdminUsuarioResponse crear(AdminUsuarioCreateRequest req) {
        String email = req.email().trim().toLowerCase();
        if (repository.existsByEmail(email)) {
            throw new BusinessException("Ya existe un usuario con ese email");
        }
        String matricula = req.matricula().trim();
        if (repository.existsByMatricula(matricula)) {
            throw new BusinessException("Ya existe un usuario con esa matrícula");
        }
        Usuario u = switch (req.rol()) {
            case ESTUDIANTE -> new Estudiante();
            case ADMINISTRADOR -> new Administrador();
            case PROFESOR -> {
                Profesor p = new Profesor();
                p.setTitulacion(trimToNull(req.titulacion()));
                p.setCargo(trimToNull(req.cargo()));
                yield p;
            }
            case EXTERNO -> {
                if (isBlank(req.institucion()) || isBlank(req.titulo())) {
                    throw new BusinessException("Institución y título son obligatorios para un externo");
                }
                Externo e = new Externo();
                e.setInstitucion(req.institucion().trim());
                e.setTitulo(req.titulo().trim());
                yield e;
            }
        };
        u.setEmail(email);
        u.setMatricula(matricula);
        u.setNombre(req.nombre().trim());
        u.setEdad(req.edad());
        u.setUbicacion(trimToNull(req.ubicacion()));
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setActivo(true);
        return toResponse(repository.save(u));
    }

    @Transactional
    public AdminUsuarioResponse actualizar(Long id, AdminUsuarioUpdateRequest req) {
        Usuario u = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        String email = req.email().trim().toLowerCase();
        if (!email.equalsIgnoreCase(u.getEmail()) && repository.existsByEmail(email)) {
            throw new BusinessException("Ya existe un usuario con ese email");
        }
        String matricula = req.matricula().trim();
        if (!matricula.equals(u.getMatricula()) && repository.existsByMatricula(matricula)) {
            throw new BusinessException("Ya existe un usuario con esa matrícula");
        }
        u.setEmail(email);
        u.setMatricula(matricula);
        u.setNombre(req.nombre().trim());
        u.setEdad(req.edad());
        u.setUbicacion(trimToNull(req.ubicacion()));
        if (req.topeAsignaciones() != null) {
            u.setTopeAsignaciones(req.topeAsignaciones());
        }
        if (u instanceof Profesor p) {
            p.setTitulacion(trimToNull(req.titulacion()));
            p.setCargo(trimToNull(req.cargo()));
        } else if (u instanceof Externo e) {
            if (isBlank(req.institucion()) || isBlank(req.titulo())) {
                throw new BusinessException("Institución y título son obligatorios para un externo");
            }
            e.setInstitucion(req.institucion().trim());
            e.setTitulo(req.titulo().trim());
        }
        return toResponse(repository.save(u));
    }

    @Transactional
    public AdminUsuarioResponse setActivo(Long id, boolean activo, Long callerId) {
        Usuario u = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        if (!activo) {
            if (u.getId().equals(callerId)) {
                throw new BusinessException("No podés desactivar tu propia cuenta");
            }
            if (u instanceof Administrador && u.isActivo() && repository.contarAdministradoresActivos() <= 1) {
                throw new BusinessException("No podés desactivar al último administrador activo");
            }
        }
        u.setActivo(activo);
        return toResponse(repository.save(u));
    }

    @Transactional
    public void resetPassword(Long id, String nuevaPassword) {
        Usuario u = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        u.setPassword(passwordEncoder.encode(nuevaPassword));
        repository.save(u);
    }

    private AdminUsuarioResponse toResponse(Usuario u) {
        String titulacion = null;
        String cargo = null;
        String institucion = null;
        String titulo = null;
        if (u instanceof Profesor p) {
            titulacion = p.getTitulacion();
            cargo = p.getCargo();
        } else if (u instanceof Externo e) {
            institucion = e.getInstitucion();
            titulo = e.getTitulo();
        }
        return new AdminUsuarioResponse(u.getId(), u.getEmail(), u.getMatricula(), u.getNombre(), u.getRol(),
                u.isActivo(), u.getEdad(), u.getUbicacion(), u.getTopeAsignaciones(),
                titulacion, cargo, institucion, titulo);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }
}
