package com.academconnect.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.TemplateEvaluacion;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.Visibilidad;
import com.academconnect.domain.VisibilidadActividad;
import com.academconnect.dto.TemplateEvaluacionRequest;
import com.academconnect.dto.TemplateEvaluacionResponse;
import com.academconnect.event.ActividadEvent;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.TemplateEvaluacionMapper;
import com.academconnect.repository.TemplateEvaluacionRepository;
import com.academconnect.repository.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TemplateEvaluacionService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Tipos válidos del campo "tipo" de cada criterio (G16). */
    private static final Set<String> TIPOS_CRITERIO = Set.of(
            "ESCALA", "SLIDER", "SELECCION", "BOOLEANO", "TEXTO");

    /** Tipos donde no hay puntaje numérico ponderable. */
    private static final Set<String> TIPOS_SIN_PONDERACION = Set.of("TEXTO");

    private final TemplateEvaluacionRepository repository;
    private final TemplateEvaluacionMapper mapper;
    private final ApplicationEventPublisher events;
    private final UsuarioRepository usuarioRepository;

    /** Rúbricas visibles paginadas por scope: "MIAS" (propias) o "PUBLICAS" (públicas de otros). */
    public Page<TemplateEvaluacionResponse> listarVisibles(Long callerId, String scope, Pageable pageable) {
        Page<TemplateEvaluacion> page = "PUBLICAS".equalsIgnoreCase(scope)
                ? repository.buscarPublicas(callerId, pageable)
                : repository.buscarMias(callerId, pageable);
        return page.map(mapper::toResponse);
    }

    public TemplateEvaluacionResponse buscarVisible(Long id, Long callerId, boolean isAdmin) {
        var template = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TemplateEvaluacion", id));
        if (!isAdmin && !esVisiblePara(template, callerId)) {
            throw new BusinessException("No tenés acceso a esta rúbrica");
        }
        return mapper.toResponse(template);
    }

    @Transactional
    public TemplateEvaluacionResponse crear(TemplateEvaluacionRequest request, Long autorId) {
        validar(request);
        var template = mapper.toEntity(request);
        var autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", autorId));
        template.setAutor(autor);
        if (template.getVisibilidad() == null) {
            template.setVisibilidad(Visibilidad.PRIVADO);
        }
        var saved = repository.save(template);
        events.publishEvent(ActividadEvent.of(
                TipoActividad.TEMPLATE_CREADO,
                autorId,
                "TEMPLATE_EVALUACION", saved.getId(),
                Map.of("nombre", saved.getNombre(),
                       "visibilidad", saved.getVisibilidad().name()),
                VisibilidadActividad.PUBLICA,
                List.of()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public TemplateEvaluacionResponse actualizar(Long id, TemplateEvaluacionRequest request, Long callerId, boolean isAdmin) {
        var template = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TemplateEvaluacion", id));
        exigirPropietarioOAdmin(template, callerId, isAdmin);
        validar(request);
        mapper.update(request, template);
        return mapper.toResponse(repository.save(template));
    }

    @Transactional
    public void desactivar(Long id, Long callerId, boolean isAdmin) {
        var template = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TemplateEvaluacion", id));
        exigirPropietarioOAdmin(template, callerId, isAdmin);
        template.setActivo(false);
        repository.save(template);
    }

    private boolean esPropietario(TemplateEvaluacion template, Long callerId) {
        return template.getAutor() != null
                && template.getAutor().getId() != null
                && template.getAutor().getId().equals(callerId);
    }

    private boolean esVisiblePara(TemplateEvaluacion template, Long callerId) {
        return esPropietario(template, callerId)
                || (template.getVisibilidad() == Visibilidad.PUBLICO && template.isActivo());
    }

    private void exigirPropietarioOAdmin(TemplateEvaluacion template, Long callerId, boolean isAdmin) {
        if (!isAdmin && !esPropietario(template, callerId)) {
            throw new BusinessException("Solo el autor o un administrador puede modificar esta rúbrica");
        }
    }

    private void validar(TemplateEvaluacionRequest request) {
        JsonNode array = parsearArray(request.criterios());
        double sumaPesos = 0;
        double escalaGlobalMin = Double.POSITIVE_INFINITY;
        double escalaGlobalMax = Double.NEGATIVE_INFINITY;

        for (JsonNode criterio : array) {
            for (String campo : new String[]{"codigo", "nombre", "tipo", "peso", "escalaMin", "escalaMax"}) {
                if (!criterio.has(campo)) {
                    throw new BusinessException("Criterio sin campo requerido: " + campo);
                }
            }
            String tipo = criterio.get("tipo").asText();
            if (!TIPOS_CRITERIO.contains(tipo)) {
                throw new BusinessException("Tipo de criterio inválido: " + tipo
                        + " (válidos: " + TIPOS_CRITERIO + ")");
            }
            if ("SELECCION".equals(tipo)) {
                if (!criterio.has("opciones") || !criterio.get("opciones").isArray()
                        || criterio.get("opciones").isEmpty()) {
                    throw new BusinessException(
                            "Criterio SELECCION requiere 'opciones' no vacío: " + criterio.get("codigo").asText());
                }
            }
            double peso = criterio.get("peso").asDouble();
            double escalaMin = criterio.get("escalaMin").asDouble();
            double escalaMax = criterio.get("escalaMax").asDouble();

            if (TIPOS_SIN_PONDERACION.contains(tipo)) {
                if (peso != 0) {
                    throw new BusinessException(
                            "Criterio tipo TEXTO no aporta puntaje y debe tener peso=0: "
                                    + criterio.get("codigo").asText());
                }
            } else {
                if (peso <= 0 || peso > 1) {
                    throw new BusinessException("El peso de cada criterio ponderable debe estar entre 0 (exclusivo) y 1");
                }
                if (escalaMin >= escalaMax) {
                    throw new BusinessException("escalaMin debe ser menor que escalaMax en criterio: "
                            + criterio.get("codigo").asText());
                }
                sumaPesos += peso;
                escalaGlobalMin = Math.min(escalaGlobalMin, escalaMin);
                escalaGlobalMax = Math.max(escalaGlobalMax, escalaMax);
            }
        }
        if (Math.abs(sumaPesos - 1.0) > 0.001) {
            throw new BusinessException(
                    "La suma de los pesos de los criterios ponderables debe ser 1.0 (actual: " + sumaPesos + ")");
        }

        BigDecimal umbral = request.umbralAprobacion();
        if (umbral == null) {
            throw new BusinessException("umbralAprobacion es requerido");
        }
        double u = umbral.doubleValue();
        if (u < escalaGlobalMin || u > escalaGlobalMax) {
            throw new BusinessException(
                    "umbralAprobacion (%s) debe estar dentro del rango [%s, %s] de los criterios ponderables"
                            .formatted(umbral, escalaGlobalMin, escalaGlobalMax));
        }
    }

    private JsonNode parsearArray(String criteriosJson) {
        JsonNode array;
        try {
            array = OBJECT_MAPPER.readTree(criteriosJson);
        } catch (JsonProcessingException e) {
            throw new BusinessException("El campo 'criterios' no es JSON válido");
        }
        if (!array.isArray() || array.isEmpty()) {
            throw new BusinessException("'criterios' debe ser un array no vacío");
        }
        return array;
    }
}
