package com.academconnect.factories;

import org.springframework.test.util.ReflectionTestUtils;

import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.UsuarioAreaTematica;
import com.academconnect.domain.UsuarioAreaTematicaId;

public class AreaTematicaFactory {

    public static AreaTematica createArea(Long id) {
        AreaTematica a = new AreaTematica();
        ReflectionTestUtils.setField(a, "id", id);
        return a;
    }

    public static UsuarioAreaTematica createUat(Long usuarioId, Long areaId) {
        UsuarioAreaTematica uat = new UsuarioAreaTematica();
        ReflectionTestUtils.setField(uat, "id", new UsuarioAreaTematicaId(usuarioId, areaId));
        return uat;
    }
}
