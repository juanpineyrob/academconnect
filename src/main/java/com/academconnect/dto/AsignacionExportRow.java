package com.academconnect.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Proyección nativa para el export CSV de métricas — una fila por asignación. */
public interface AsignacionExportRow {
    String getTrabajoTitulo();
    String getTrabajoTipo();
    String getAreas();
    String getInstanciaNombre();
    String getEvaluadorNombre();
    String getEstadoAsignacion();
    Instant getAsignadaEn();
    Instant getVencimientoEn();
    BigDecimal getCalificacionFinal();
    String getEstadoEvaluacion();
}
