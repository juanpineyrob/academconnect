package com.academconnect.factories;

import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;

import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.Profesor;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.Trabajo;

public class TrabajoFactory {

    public static Trabajo createTrabajo() {
        return createTrabajo(1L, TipoTrabajo.TCC, EstadoTrabajo.EN_EVALUACION);
    }

    public static Trabajo createTrabajo(Long id, TipoTrabajo tipo, EstadoTrabajo estado) {
        Profesor orientador = UsuarioFactory.createProfesor();
        return createTrabajo(id, tipo, estado, orientador);
    }

    public static Trabajo createTrabajo(Long id, TipoTrabajo tipo, EstadoTrabajo estado, Profesor orientador) {
        Trabajo t = new Trabajo();
        ReflectionTestUtils.setField(t, "id", id);
        t.setTitulo("Trabajo " + id);
        t.setDescripcion("descripcion");
        t.setTipo(tipo);
        t.setEstado(estado);
        t.setOrientador(orientador);
        t.setKeywords(List.of("kw1", "kw2", "kw3"));
        return t;
    }
}
