package com.academconnect.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.DisponibilidadEvaluador;
import com.academconnect.domain.Usuario;
import com.academconnect.dto.DisponibilidadRequest;
import com.academconnect.dto.DisponibilidadResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.DisponibilidadEvaluadorMapper;
import com.academconnect.repository.DisponibilidadEvaluadorRepository;
import com.academconnect.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/** F14 G23 — disponibilidad horaria del evaluador (heatmap mensual). */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DisponibilidadEvaluadorService {

    private final DisponibilidadEvaluadorRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadEvaluadorMapper mapper;

    public List<DisponibilidadResponse> listarRango(String email, LocalDate desde, LocalDate hasta) {
        Usuario u = usuarioByEmail(email);
        return repository.findByEvaluadorIdAndFechaBetween(u.getId(), desde, hasta).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<DisponibilidadResponse> guardar(String email, DisponibilidadRequest request) {
        Usuario u = usuarioByEmail(email);
        LocalDate hoy = LocalDate.now();
        List<DisponibilidadResponse> resultado = new java.util.ArrayList<>();
        for (var item : request.items()) {
            if (item.fecha().isBefore(hoy)) {
                throw new BusinessException(
                        "No se puede declarar disponibilidad para fechas pasadas: " + item.fecha());
            }
            DisponibilidadEvaluador entity = repository
                    .findByEvaluadorIdAndFecha(u.getId(), item.fecha())
                    .orElseGet(() -> {
                        DisponibilidadEvaluador d = new DisponibilidadEvaluador();
                        d.setEvaluador(u);
                        d.setFecha(item.fecha());
                        return d;
                    });
            entity.setHorasDisponibles(item.horasDisponibles());
            resultado.add(toResponse(repository.save(entity)));
        }
        return resultado;
    }

    private Usuario usuarioByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email));
    }

    private DisponibilidadResponse toResponse(DisponibilidadEvaluador d) {
        return mapper.toResponse(d);
    }
}
