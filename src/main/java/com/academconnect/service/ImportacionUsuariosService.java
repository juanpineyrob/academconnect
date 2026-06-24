package com.academconnect.service;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.EstadoCuenta;
import com.academconnect.domain.EstadoLote;
import com.academconnect.domain.LoteImportacion;
import com.academconnect.domain.LoteImportacionItem;
import com.academconnect.domain.ResultadoFila;
import com.academconnect.dto.ImportItemResponse;
import com.academconnect.dto.ImportPreviewResponse;
import com.academconnect.exception.BusinessException;
import com.academconnect.repository.LoteImportacionRepository;
import com.academconnect.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportacionUsuariosService {

    private final UsuarioRepository usuarioRepository;
    private final LoteImportacionRepository loteRepository;

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
                } else {
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
