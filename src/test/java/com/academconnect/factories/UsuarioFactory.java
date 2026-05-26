package com.academconnect.factories;

import org.springframework.test.util.ReflectionTestUtils;

import com.academconnect.domain.Estudiante;
import com.academconnect.domain.Externo;
import com.academconnect.domain.Profesor;

public class UsuarioFactory {

    public static Estudiante createEstudiante() {
        return createEstudiante(1L, "alumno@academconnect.com");
    }

    public static Estudiante createEstudiante(Long id, String email) {
        Estudiante e = new Estudiante();
        ReflectionTestUtils.setField(e, "id", id);
        e.setEmail(email);
        e.setPassword("encoded");
        e.setNombre("Estudiante " + id);
        e.setActivo(true);
        return e;
    }

    public static Profesor createProfesor() {
        return createProfesor(1L, "profesor@academconnect.com");
    }

    public static Profesor createProfesor(Long id, String email) {
        Profesor p = new Profesor();
        ReflectionTestUtils.setField(p, "id", id);
        p.setEmail(email);
        p.setPassword("encoded");
        p.setNombre("Profesor " + id);
        p.setActivo(true);
        p.setTopeAsignaciones(10);
        return p;
    }

    public static Externo createExterno(Long id, String email) {
        Externo e = new Externo();
        ReflectionTestUtils.setField(e, "id", id);
        e.setEmail(email);
        e.setPassword("encoded");
        e.setNombre("Externo " + id);
        e.setActivo(true);
        return e;
    }
}
