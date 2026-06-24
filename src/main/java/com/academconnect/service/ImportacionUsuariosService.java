package com.academconnect.service;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.EstadoCuenta;
import com.academconnect.domain.EstadoLote;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.LoteImportacion;
import com.academconnect.domain.LoteImportacionItem;
import com.academconnect.domain.PropositoToken;
import com.academconnect.domain.ResultadoFila;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.VisibilidadActividad;
import com.academconnect.dto.ImportConfirmRequest;
import com.academconnect.dto.ImportItemResponse;
import com.academconnect.dto.ImportPreviewResponse;
import com.academconnect.event.ActividadEvent;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.repository.LoteImportacionRepository;
import com.academconnect.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportacionUsuariosService {

    private final UsuarioRepository usuarioRepository;
    private final LoteImportacionRepository loteRepository;
    private final TokenCuentaService tokenService;
    private final MailService mailService;
    private final MailTemplateService templates;
    private final ApplicationEventPublisher eventos;

    /**
     * Dry-run: parsea el CSV (cabecera {@code email,matricula,nombre}), clasifica cada fila contra el
     * padrón real (findByEmail/findByMatricula) y persiste un {@link LoteImportacion} en estado PREVIEW
     * con sus items. NO crea usuarios.
     */
    @Transactional
    public ImportPreviewResponse preview(String nombreArchivo, byte[] contenido, Long adminId) {
        LoteImportacion lote = new LoteImportacion();
        lote.setNombreArchivo(nombreArchivo == null ? "import.csv" : nombreArchivo);
        lote.setArchivoHash(sha256(contenido));
        lote.setEstado(EstadoLote.PREVIEW);
        lote.setCreadoPorId(adminId);

        List<ImportItemResponse> respuestas = new ArrayList<>();
        int nuevos = 0, existentes = 0, errores = 0, total = 0;
        // Dedup intra-archivo: misma normalización que los lookups (email lower+trim, matrícula trim).
        Set<String> emailsVistos = new HashSet<>();
        Set<String> matriculasVistas = new HashSet<>();

        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setTrim(true)
                .setIgnoreEmptyLines(true).build()
                .parse(new StringReader(new String(contenido, StandardCharsets.UTF_8)))) {

            for (CSVRecord r : parser) {
                total++;
                LoteImportacionItem item = new LoteImportacionItem();
                item.setLote(lote);
                item.setLinea((int) r.getRecordNumber() + 1); // +1 por la cabecera

                String email, matricula, nombre;
                try {
                    email = r.get("email").trim().toLowerCase();
                    matricula = r.get("matricula").trim();
                    nombre = r.get("nombre").trim();
                } catch (IllegalArgumentException ex) {
                    item.setResultado(ResultadoFila.ERROR_FORMATO);
                    item.setDetalle("Columnas email,matricula,nombre requeridas");
                    errores++;
                    lote.getItems().add(item);
                    respuestas.add(toResp(item));
                    continue;
                }

                if (email.isBlank() || matricula.isBlank() || nombre.isBlank()) {
                    item.setResultado(ResultadoFila.ERROR_FORMATO);
                    item.setDetalle("Campos vacíos");
                    errores++;
                } else if (emailsVistos.contains(email)) {
                    item.setEmail(email);
                    item.setMatricula(matricula);
                    item.setNombre(nombre);
                    item.setResultado(ResultadoFila.COLISION_EMAIL);
                    item.setDetalle("Email duplicado en el archivo");
                    errores++;
                } else if (matriculasVistas.contains(matricula)) {
                    item.setEmail(email);
                    item.setMatricula(matricula);
                    item.setNombre(nombre);
                    item.setResultado(ResultadoFila.COLISION_MATRICULA);
                    item.setDetalle("Matrícula duplicada en el archivo");
                    errores++;
                } else {
                    emailsVistos.add(email);
                    matriculasVistas.add(matricula);
                    item.setEmail(email);
                    item.setMatricula(matricula);
                    item.setNombre(nombre);
                    var porEmail = usuarioRepository.findByEmail(email);
                    var porMatricula = usuarioRepository.findByMatricula(matricula);
                    if (porEmail.isPresent() && porMatricula.isPresent()
                            && porEmail.get().getId().equals(porMatricula.get().getId())) {
                        // par exacto -> ya existe
                        item.setResultado(porEmail.get().getEstadoCuenta() == EstadoCuenta.ACTIVA
                                ? ResultadoFila.EXISTE_ACTIVA : ResultadoFila.EXISTE_INVITADA);
                        existentes++;
                    } else if (porMatricula.isPresent()) {
                        item.setResultado(ResultadoFila.COLISION_MATRICULA);
                        item.setDetalle("La matrícula ya pertenece a otro email");
                        errores++;
                    } else if (porEmail.isPresent()) {
                        item.setResultado(ResultadoFila.COLISION_EMAIL);
                        item.setDetalle("El email ya pertenece a otra matrícula");
                        errores++;
                    } else {
                        item.setResultado(ResultadoFila.NUEVO);
                        nuevos++;
                    }
                }
                lote.getItems().add(item);
                respuestas.add(toResp(item));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("El archivo CSV es inválido o está corrupto");
        }

        lote.setTotal(total);
        lote.setNuevos(nuevos);
        lote.setExistentes(existentes);
        lote.setErrores(errores);
        var guardado = loteRepository.save(lote);
        return new ImportPreviewResponse(guardado.getId(), total, nuevos, existentes, errores, respuestas);
    }

    /**
     * Commit del lote: crea las cuentas NUEVO como INVITADA (vinculadas al lote), emite token de
     * ACTIVACION y encola el mail. Las EXISTE_INVITADA se reenvían solo si {@code reenviarInvitadas}.
     * El resto (EXISTE_ACTIVA, colisiones, errores) se saltea (no merge, no overwrite). Idempotente:
     * un re-preview tras el commit clasifica las creadas como EXISTE_INVITADA → no se recrean.
     */
    @Transactional
    public void confirmar(Long loteId, ImportConfirmRequest req, Long adminId) {
        LoteImportacion lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote de importación", loteId));
        if (lote.getEstado() == EstadoLote.CONFIRMADO) {
            throw new BusinessException("El lote ya fue confirmado");
        }
        for (LoteImportacionItem item : lote.getItems()) {
            switch (item.getResultado()) {
                case NUEVO -> {
                    // Re-chequeo TOCTOU: la cuenta pudo crearse entre el preview y el confirm.
                    if (usuarioRepository.existsByEmail(item.getEmail())
                            || usuarioRepository.existsByMatricula(item.getMatricula())) {
                        break;
                    }
                    Estudiante u = new Estudiante();
                    u.setEmail(item.getEmail());
                    u.setMatricula(item.getMatricula());
                    u.setNombre(item.getNombre());
                    u.setActivo(true);
                    u.setEstadoCuenta(EstadoCuenta.INVITADA);
                    u.setPassword(null);
                    u.setLoteImportacionId(lote.getId());
                    var g = usuarioRepository.save(u);
                    String token = tokenService.emitir(g.getId(), PropositoToken.ACTIVACION);
                    var c = templates.activacion(g.getNombre(), token);
                    mailService.encolar(g.getEmail(), c.asunto(), c.html(), c.texto());
                }
                case EXISTE_INVITADA -> {
                    if (req.reenviarInvitadas()) {
                        var u = usuarioRepository.findByEmail(item.getEmail()).orElseThrow();
                        String token = tokenService.emitir(u.getId(), PropositoToken.ACTIVACION);
                        var c = templates.activacion(u.getNombre(), token);
                        mailService.encolar(u.getEmail(), c.asunto(), c.html(), c.texto());
                    }
                }
                default -> { /* EXISTE_ACTIVA, colisiones, errores -> skip (no merge, no overwrite) */ }
            }
        }
        lote.setEstado(EstadoLote.CONFIRMADO);
        eventos.publishEvent(ActividadEvent.of(TipoActividad.IMPORTACION_CONFIRMADA, adminId,
                "LOTE_IMPORTACION", lote.getId(),
                Map.of("total", lote.getTotal(), "nuevos", lote.getNuevos(),
                        "existentes", lote.getExistentes(), "errores", lote.getErrores(),
                        "archivoHash", lote.getArchivoHash()),
                VisibilidadActividad.PRIVADA, List.of()));
    }

    private ImportItemResponse toResp(LoteImportacionItem i) {
        return new ImportItemResponse(i.getLinea(), i.getMatricula(), i.getEmail(), i.getNombre(),
                i.getResultado(), i.getDetalle());
    }

    private String sha256(byte[] b) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
