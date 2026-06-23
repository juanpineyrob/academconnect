package com.academconnect.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.EstadoCuenta;
import com.academconnect.domain.EstadoSolicitudCuenta;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.PropositoToken;
import com.academconnect.domain.SolicitudCuenta;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.Usuario;
import com.academconnect.domain.VisibilidadActividad;
import com.academconnect.dto.SolicitudResponse;
import com.academconnect.event.ActividadEvent;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.exception.TokenInvalidoException;
import com.academconnect.repository.SolicitudCuentaRepository;
import com.academconnect.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final TokenCuentaService tokenService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SolicitudCuentaRepository solicitudRepository;
    private final MailService mailService;
    private final MailTemplateService templates;
    private final ApplicationEventPublisher eventos;

    /** Consume el token (ACTIVACION o RESET) y fija la contraseña; activa la cuenta si era ACTIVACION. */
    @Transactional
    public void establecerPassword(String tokenClaro, String passwordPlano) {
        PropositoToken proposito = tokenService.propositoSiUsable(tokenClaro);
        if (proposito == null) {
            throw new TokenInvalidoException();
        }
        Usuario u = tokenService.consumir(tokenClaro, proposito);
        u.setPassword(passwordEncoder.encode(passwordPlano));
        if (proposito == PropositoToken.ACTIVACION) {
            u.setEstadoCuenta(EstadoCuenta.ACTIVA);
        }
        usuarioRepository.save(u);
    }

    /** Self-request: registra una solicitud de cuenta. Siempre silenciosa hacia afuera. */
    @Transactional
    public void crearSolicitud(String matricula, String email, String nombre) {
        SolicitudCuenta s = new SolicitudCuenta();
        s.setMatricula(matricula.trim());
        s.setEmail(email.trim().toLowerCase());
        s.setNombre(nombre.trim());
        solicitudRepository.save(s);
        eventos.publishEvent(ActividadEvent.of(
                TipoActividad.SOLICITUD_CUENTA_ENVIADA, null, "SOLICITUD_CUENTA", s.getId(),
                Map.of("matricula", s.getMatricula()),
                VisibilidadActividad.PRIVADA, List.of()));
    }

    /** Emite RESET solo si la cuenta existe y está ACTIVA; encola mail. Silencioso si no aplica. */
    @Transactional
    public void solicitarReset(String email) {
        usuarioRepository.findByEmail(email.trim().toLowerCase())
                .filter(u -> u.getEstadoCuenta() == EstadoCuenta.ACTIVA)
                .ifPresent(u -> {
                    String token = tokenService.emitir(u.getId(), PropositoToken.RESET);
                    var c = templates.restablecer(u.getNombre(), token);
                    mailService.encolar(u.getEmail(), c.asunto(), c.html(), c.texto());
                });
    }

    /** Reemite ACTIVACION solo si la cuenta existe y está INVITADA; encola mail. Silencioso si no aplica. */
    @Transactional
    public void reenviarActivacion(String email) {
        usuarioRepository.findByEmail(email.trim().toLowerCase())
                .filter(u -> u.getEstadoCuenta() == EstadoCuenta.INVITADA)
                .ifPresent(u -> {
                    String token = tokenService.emitir(u.getId(), PropositoToken.ACTIVACION);
                    var c = templates.activacion(u.getNombre(), token);
                    mailService.encolar(u.getEmail(), c.asunto(), c.html(), c.texto());
                });
    }

    /** Admin aprueba una solicitud: crea cuenta INVITADA (Estudiante) + token ACTIVACION + encola mail. */
    @Transactional
    public SolicitudResponse aprobar(Long solicitudId, Long adminId) {
        var s = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de cuenta", solicitudId));
        String email = s.getEmail().toLowerCase();
        if (usuarioRepository.findByEmail(email).isPresent()
                || usuarioRepository.existsByMatricula(s.getMatricula())) {
            throw new BusinessException("Ya existe una cuenta con ese email o matrícula");
        }
        Estudiante u = new Estudiante();
        u.setEmail(email);
        u.setMatricula(s.getMatricula());
        u.setNombre(s.getNombre());
        u.setActivo(true);
        u.setEstadoCuenta(EstadoCuenta.INVITADA);
        u.setPassword(null);
        usuarioRepository.save(u);

        s.setEstado(EstadoSolicitudCuenta.APROBADA);
        s.setDecididoPorId(adminId);
        s.setDecididoEn(Instant.now());

        String token = tokenService.emitir(u.getId(), PropositoToken.ACTIVACION);
        var c = templates.activacion(u.getNombre(), token);
        mailService.encolar(u.getEmail(), c.asunto(), c.html(), c.texto());

        eventos.publishEvent(ActividadEvent.of(TipoActividad.SOLICITUD_CUENTA_APROBADA, adminId,
                "SOLICITUD_CUENTA", s.getId(), Map.of("matricula", s.getMatricula()),
                VisibilidadActividad.PRIVADA, List.of()));
        eventos.publishEvent(ActividadEvent.of(TipoActividad.CUENTA_INVITADA_CREADA, adminId,
                "USUARIO", u.getId(), Map.of("matricula", u.getMatricula()),
                VisibilidadActividad.PRIVADA, List.of()));
        return toResponse(s);
    }

    /** Admin rechaza una solicitud, conservando el motivo. */
    @Transactional
    public SolicitudResponse rechazar(Long solicitudId, Long adminId, String motivo) {
        var s = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de cuenta", solicitudId));
        s.setEstado(EstadoSolicitudCuenta.RECHAZADA);
        s.setMotivoRechazo(motivo);
        s.setDecididoPorId(adminId);
        s.setDecididoEn(Instant.now());
        eventos.publishEvent(ActividadEvent.of(TipoActividad.SOLICITUD_CUENTA_RECHAZADA, adminId,
                "SOLICITUD_CUENTA", s.getId(), Map.of("matricula", s.getMatricula()),
                VisibilidadActividad.PRIVADA, List.of()));
        return toResponse(s);
    }

    @Transactional(readOnly = true)
    public Page<SolicitudResponse> buscar(EstadoSolicitudCuenta estado, String q, Pageable pageable) {
        String patron = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        return solicitudRepository.buscar(estado, patron, pageable).map(this::toResponse);
    }

    private SolicitudResponse toResponse(SolicitudCuenta s) {
        return new SolicitudResponse(s.getId(), s.getMatricula(), s.getEmail(), s.getNombre(),
                s.getEstado(), s.getMotivoRechazo(), s.getCreatedAt());
    }
}
