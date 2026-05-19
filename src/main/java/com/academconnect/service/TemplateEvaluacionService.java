package com.academconnect.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.dto.TemplateEvaluacionRequest;
import com.academconnect.dto.TemplateEvaluacionResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.TemplateEvaluacionMapper;
import com.academconnect.repository.TemplateEvaluacionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TemplateEvaluacionService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TemplateEvaluacionRepository repository;
    private final TemplateEvaluacionMapper mapper;

    public List<TemplateEvaluacionResponse> listar() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    public List<TemplateEvaluacionResponse> listarActivos() {
        return repository.findAll().stream()
                .filter(t -> t.isActivo())
                .map(mapper::toResponse)
                .toList();
    }

    public TemplateEvaluacionResponse buscarPorId(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("TemplateEvaluacion", id));
    }

    @Transactional
    public TemplateEvaluacionResponse crear(TemplateEvaluacionRequest request) {
        validarCriterios(request.criterios());
        var template = mapper.toEntity(request);
        return mapper.toResponse(repository.save(template));
    }

    @Transactional
    public TemplateEvaluacionResponse actualizar(Long id, TemplateEvaluacionRequest request) {
        var template = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TemplateEvaluacion", id));
        validarCriterios(request.criterios());
        mapper.update(request, template);
        return mapper.toResponse(repository.save(template));
    }

    @Transactional
    public void desactivar(Long id) {
        var template = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TemplateEvaluacion", id));
        template.setActivo(false);
        repository.save(template);
    }

    private void validarCriterios(String criteriosJson) {
        JsonNode array;
        try {
            array = OBJECT_MAPPER.readTree(criteriosJson);
        } catch (JsonProcessingException e) {
            throw new BusinessException("El campo 'criterios' no es JSON válido");
        }
        if (!array.isArray() || array.isEmpty()) {
            throw new BusinessException("'criterios' debe ser un array no vacío");
        }
        double sumaPesos = 0;
        for (JsonNode criterio : array) {
            for (String campo : new String[]{"codigo", "nombre", "peso", "escalaMin", "escalaMax"}) {
                if (!criterio.has(campo)) {
                    throw new BusinessException("Criterio sin campo requerido: " + campo);
                }
            }
            double peso = criterio.get("peso").asDouble();
            double escalaMin = criterio.get("escalaMin").asDouble();
            double escalaMax = criterio.get("escalaMax").asDouble();
            if (peso <= 0 || peso > 1) {
                throw new BusinessException("El peso de cada criterio debe estar entre 0 (exclusivo) y 1");
            }
            if (escalaMin >= escalaMax) {
                throw new BusinessException("escalaMin debe ser menor que escalaMax en criterio: "
                        + criterio.get("codigo").asText());
            }
            sumaPesos += peso;
        }
        if (Math.abs(sumaPesos - 1.0) > 0.001) {
            throw new BusinessException("La suma de los pesos de los criterios debe ser 1.0 (actual: " + sumaPesos + ")");
        }
    }
}
