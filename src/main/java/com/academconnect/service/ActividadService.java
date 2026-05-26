package com.academconnect.service;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.Actividad;
import com.academconnect.dto.ActividadResponse;
import com.academconnect.event.ActividadEvent;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.ActividadMapper;
import com.academconnect.repository.ActividadRepository;
import com.academconnect.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** F15 — persistencia y consulta del feed de actividad. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ActividadService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ActividadRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final ActividadMapper mapper;

    /** Listener async: cada ActividadEvent emitido por un service se persiste fuera del flujo de negocio. */
    @Async("actividadExecutor")
    @EventListener
    @Transactional
    public void persistir(ActividadEvent event) {
        try {
            Actividad a = new Actividad();
            a.setTipo(event.tipo());
            a.setActorId(event.actorId());
            a.setRecursoTipo(event.recursoTipo());
            a.setRecursoId(event.recursoId());
            a.setPayload(OBJECT_MAPPER.writeValueAsString(event.payload()));
            a.setVisibilidad(event.visibilidad());
            a.setParticipantesIds(event.participantesIds());
            repository.save(a);
        } catch (Exception e) {
            log.warn("No se pudo persistir actividad {}: {}", event.tipo(), e.getMessage());
        }
    }

    public List<ActividadResponse> feed(String email, int limit) {
        var usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email));
        return repository.feedDelUsuario(usuario.getId(), PageRequest.of(0, Math.min(limit, 100))).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ActividadResponse> feedAdmin(int limit) {
        return repository.findAll(PageRequest.of(0, Math.min(limit, 100), org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))).stream()
                .map(this::toResponse)
                .toList();
    }

    private ActividadResponse toResponse(Actividad a) {
        return mapper.toResponse(a);
    }
}
