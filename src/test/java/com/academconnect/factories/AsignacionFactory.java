package com.academconnect.factories;

import java.time.Instant;

import org.springframework.test.util.ReflectionTestUtils;

import com.academconnect.domain.Asignacion;
import com.academconnect.domain.EstadoAsignacion;
import com.academconnect.domain.Trabajo;
import com.academconnect.domain.Usuario;
import com.academconnect.domain.Versionamiento;

public class AsignacionFactory {

    public static Asignacion createAsignacionActiva(Long id, Trabajo trabajo, Usuario evaluador, String templateSnapshot) {
        Versionamiento version = new Versionamiento();
        ReflectionTestUtils.setField(version, "id", 20L);

        Asignacion asignacion = new Asignacion();
        asignacion.setTrabajo(trabajo);
        asignacion.setVersionamiento(version);
        asignacion.setEvaluador(evaluador);
        asignacion.setEstado(EstadoAsignacion.ACTIVA);
        asignacion.setTemplateSnapshot(templateSnapshot);
        asignacion.setAsignadaEn(Instant.now());
        ReflectionTestUtils.setField(asignacion, "id", id);
        return asignacion;
    }

    public static String snapshotConUmbral(double umbral) {
        return """
                {
                  "criterios": [
                    {"codigo":"C1","nombre":"Claridad","tipo":"ESCALA","peso":0.6,"escalaMin":0,"escalaMax":10},
                    {"codigo":"C2","nombre":"Originalidad","tipo":"ESCALA","peso":0.4,"escalaMin":0,"escalaMax":10}
                  ],
                  "umbralAprobacion": %s
                }
                """.formatted(umbral);
    }
}
