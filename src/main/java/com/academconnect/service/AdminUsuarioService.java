package com.academconnect.service;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.Administrador;
import com.academconnect.domain.EstadoCuenta;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.Externo;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.PropositoToken;
import com.academconnect.domain.Rol;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.Usuario;
import com.academconnect.domain.VisibilidadActividad;
import com.academconnect.dto.AdminUsuarioCreateRequest;
import com.academconnect.dto.AdminUsuarioResponse;
import com.academconnect.dto.AdminUsuarioUpdateRequest;
import com.academconnect.event.ActividadEvent;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminUsuarioService {

    private final UsuarioRepository repository;
    private final TokenCuentaService tokenService;
    private final MailService mailService;
    private final MailTemplateService templates;
    private final ApplicationEventPublisher eventos;

    public Page<AdminUsuarioResponse> buscar(String q, Rol rol, Pageable pageable) {
        String patron = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        Page<Usuario> page = (rol == null)
                ? repository.buscarAdmin(patron, pageable)
                : repository.buscarAdminPorTipo(patron, claseDe(rol), pageable);
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
        u.setActivo(true);
        u.setEstadoCuenta(EstadoCuenta.INVITADA);
        u.setPassword(null);
        Usuario guardado = repository.save(u);

        String token = tokenService.emitir(guardado.getId(), PropositoToken.ACTIVACION);
        var c = templates.activacion(guardado.getNombre(), token);
        mailService.encolar(guardado.getEmail(), c.asunto(), c.html(), c.texto());

        eventos.publishEvent(ActividadEvent.of(TipoActividad.CUENTA_INVITADA_CREADA, null,
                "USUARIO", guardado.getId(), Map.of("matricula", guardado.getMatricula()),
                VisibilidadActividad.PRIVADA, List.of()));
        return toResponse(guardado);
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

    /**
     * Envía al usuario un enlace para establecer su contraseña. Emite RESET si la cuenta está ACTIVA
     * o ACTIVACION si todavía está INVITADA; encola el mail correspondiente. El admin nunca fija
     * contraseñas: la única ancla de identidad es el control del correo institucional.
     */
    @Transactional
    public void enviarEnlacePassword(Long id) {
        Usuario u = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        PropositoToken proposito = (u.getEstadoCuenta() == EstadoCuenta.ACTIVA)
                ? PropositoToken.RESET
                : PropositoToken.ACTIVACION;
        String token = tokenService.emitir(u.getId(), proposito);
        var c = (proposito == PropositoToken.RESET)
                ? templates.restablecer(u.getNombre(), token)
                : templates.activacion(u.getNombre(), token);
        mailService.encolar(u.getEmail(), c.asunto(), c.html(), c.texto());
        eventos.publishEvent(ActividadEvent.of(TipoActividad.ENLACE_PASSWORD_ENVIADO, null,
                "USUARIO", u.getId(), Map.of("proposito", proposito.name()),
                VisibilidadActividad.PRIVADA, List.of()));
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
