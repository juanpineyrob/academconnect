package com.academconnect.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.EstadoAsignacion;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.dto.AsignacionExportRow;
import com.academconnect.dto.CargaEvaluadorDto;
import com.academconnect.dto.CargaEvaluadorResponse;
import com.academconnect.dto.MetricasResponse;
import com.academconnect.dto.StatsEvaluadorResponse;
import com.academconnect.dto.StatsPublicasResponse;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.DisponibilidadEvaluadorRepository;
import com.academconnect.repository.EvaluacionRepository;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MetricasService {

    private final TrabajoRepository trabajoRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final AsignacionRepository asignacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadEvaluadorRepository disponibilidadRepository;

    /** G08 — carga actual del evaluador vs su tope, + disponibilidad semanal próxima (cruza G23). */
    public CargaEvaluadorResponse cargaEvaluador(String email) {
        var evaluador = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email));
        long activas = asignacionRepository.countByEvaluadorIdAndEstado(
                evaluador.getId(), EstadoAsignacion.ACTIVA);
        int tope = evaluador.getTopeAsignaciones();
        double porcentaje = tope > 0 ? (double) activas * 100.0 / tope : 0.0;
        LocalDate hoy = LocalDate.now();
        BigDecimal disponibleSemanal = disponibilidadRepository.totalHoras(
                evaluador.getId(), hoy, hoy.plusDays(6));
        return new CargaEvaluadorResponse(activas, tope, porcentaje, disponibleSemanal);
    }

    /** G22 — estadísticas del evaluador autenticado (B+ del plan: sin "acuerdo con par"). */
    public StatsEvaluadorResponse statsEvaluador(String email) {
        var evaluador = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", email));
        long completadas = evaluacionRepository.countCompletadasPorEvaluador(evaluador.getId());
        Double horasMedias = evaluacionRepository.tiempoMedioHorasPorEvaluador(evaluador.getId());
        BigDecimal dias = horasMedias == null
                ? null
                : BigDecimal.valueOf(horasMedias / 24.0).setScale(2, RoundingMode.HALF_UP);
        BigDecimal scoreMedio = evaluacionRepository.scoreMedioPorEvaluador(evaluador.getId());
        if (scoreMedio != null) {
            scoreMedio = scoreMedio.setScale(2, RoundingMode.HALF_UP);
        }
        long aprobados = trabajoRepository.countAprobadosConEvaluadoresIncluyendo(evaluador.getId());
        long rechazados = trabajoRepository.countRechazadosConEvaluadoresIncluyendo(evaluador.getId());
        return new StatsEvaluadorResponse(completadas, dias, scoreMedio, aprobados, rechazados);
    }

    /** G02 — métricas agregadas visibles sin autenticación. */
    public StatsPublicasResponse statsPublicas() {
        long trabajosPublicados = trabajoRepository.countByEstado(EstadoTrabajo.APROBADO);
        long areasActivas = trabajoRepository.countAreasDistintasConTrabajosEnEstados(List.of(EstadoTrabajo.APROBADO));
        long evaluadoresActivos = usuarioRepository.contarEvaluadoresActivos();
        return new StatsPublicasResponse(trabajosPublicados, areasActivas, evaluadoresActivos);
    }

    public MetricasResponse obtenerMetricas() {
        var trabajosPorEstado = trabajoRepository.contarPorEstado();
        var tiempoPromedio = evaluacionRepository.promedioTiempoEvaluacionHoras();
        var cargaPorEvaluador = asignacionRepository.cargaActivaPorEvaluador();
        double gini = calcularGini(cargaPorEvaluador.stream().map(CargaEvaluadorDto::cargaActiva).toList());
        return new MetricasResponse(trabajosPorEstado, tiempoPromedio, cargaPorEvaluador, gini);
    }

    private static final String[] CABECERA_EXPORT = {
        "Trabajo", "Tipo", "Areas", "Instancia", "Evaluador", "Estado asignacion",
        "Asignada en", "Vencimiento", "Calificacion final", "Estado evaluacion",
    };

    /** Detalle crudo (una fila por asignación) para descargar en Excel/CSV. */
    public String exportarCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF").append(String.join(",", CABECERA_EXPORT)).append('\n');
        for (AsignacionExportRow r : asignacionRepository.exportarFilas()) {
            sb.append(csv(r.getTrabajoTitulo())).append(',')
              .append(csv(r.getTrabajoTipo())).append(',')
              .append(csv(r.getAreas())).append(',')
              .append(csv(r.getInstanciaNombre())).append(',')
              .append(csv(r.getEvaluadorNombre())).append(',')
              .append(csv(r.getEstadoAsignacion())).append(',')
              .append(csv(r.getAsignadaEn() == null ? null : r.getAsignadaEn().toString())).append(',')
              .append(csv(r.getVencimientoEn() == null ? null : r.getVencimientoEn().toString())).append(',')
              .append(csv(r.getCalificacionFinal() == null ? null : r.getCalificacionFinal().toString())).append(',')
              .append(csv(r.getEstadoEvaluacion())).append('\n');
        }
        return sb.toString();
    }

    private String csv(String valor) {
        if (valor == null) return "";
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    /**
     * Gini coefficient for evaluator load distribution.
     * Returns 0.0 when all evaluators have equal load; approaches 1.0 under extreme concentration.
     */
    private double calcularGini(List<Long> cargas) {
        if (cargas.isEmpty()) return 0.0;
        List<Long> sorted = cargas.stream().sorted().toList();
        long n = sorted.size();
        long sumTotal = sorted.stream().mapToLong(Long::longValue).sum();
        if (sumTotal == 0) return 0.0;
        long weightedSum = 0;
        for (int i = 0; i < sorted.size(); i++) {
            weightedSum += (long)(i + 1) * sorted.get(i);
        }
        return (double)(2 * weightedSum) / ((double) n * sumTotal) - (double)(n + 1) / n;
    }
}
