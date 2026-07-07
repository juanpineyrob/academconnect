package com.academconnect.dto;

/**
 * Selección de rúbrica por el evaluador al entrar a la evaluación.
 * Si {@code templateEvaluacionId} es null, se usa la rúbrica por defecto.
 */
public record SeleccionRubricaRequest(Long templateEvaluacionId) {
}
