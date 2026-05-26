package com.academconnect.service;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.Reconocimiento;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.Usuario;
import com.academconnect.domain.VisibilidadActividad;
import com.academconnect.dto.ReconocimientoRequest;
import com.academconnect.dto.ReconocimientoResponse;
import com.academconnect.event.ActividadEvent;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.ReconocimientoMapper;
import com.academconnect.repository.ReconocimientoRepository;
import com.academconnect.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/** F14 G24 — reconocimientos/badges (gestionados por admin, visibles en perfil público). */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReconocimientoService {

    private final ReconocimientoRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher events;
    private final ReconocimientoMapper mapper;

    public List<ReconocimientoResponse> listarDeUsuario(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuario", usuarioId);
        }
        return repository.findByUsuarioIdOrderByAnioDesc(usuarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReconocimientoResponse otorgar(Long usuarioId, String otorganteEmail, ReconocimientoRequest request) {
        Usuario destinatario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));
        Usuario otorgante = usuarioRepository.findByEmail(otorganteEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", otorganteEmail));

        Reconocimiento r = new Reconocimiento();
        r.setUsuario(destinatario);
        r.setTipo(request.tipo());
        r.setDescripcion(request.descripcion());
        r.setAnio(request.anio());
        r.setOtorgadoPor(otorgante);
        Reconocimiento saved = repository.save(r);

        events.publishEvent(ActividadEvent.of(
                TipoActividad.RECONOCIMIENTO_OTORGADO,
                otorgante.getId(),
                "RECONOCIMIENTO", saved.getId(),
                Map.of("destinatarioId", destinatario.getId(),
                       "destinatarioNombre", destinatario.getNombre(),
                       "descripcion", request.descripcion(),
                       "anio", request.anio()),
                VisibilidadActividad.PUBLICA,
                List.of(destinatario.getId())));

        return toResponse(saved);
    }

    @Transactional
    public void revocar(Long id) {
        Reconocimiento r = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reconocimiento", id));
        repository.deleteById(id);
        events.publishEvent(ActividadEvent.of(
                TipoActividad.RECONOCIMIENTO_REVOCADO,
                null,
                "RECONOCIMIENTO", id,
                Map.of("destinatarioId", r.getUsuario().getId(),
                       "destinatarioNombre", r.getUsuario().getNombre(),
                       "descripcion", r.getDescripcion()),
                VisibilidadActividad.PARTICIPANTES,
                List.of(r.getUsuario().getId())));
    }

    private ReconocimientoResponse toResponse(Reconocimiento r) {
        return mapper.toResponse(r);
    }
}
