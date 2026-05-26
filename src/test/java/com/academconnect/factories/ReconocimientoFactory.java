package com.academconnect.factories;

import org.springframework.test.util.ReflectionTestUtils;

import com.academconnect.domain.Reconocimiento;
import com.academconnect.domain.Usuario;
import com.academconnect.dto.ReconocimientoRequest;

public class ReconocimientoFactory {

    public static Reconocimiento createReconocimiento(Long id, Usuario destinatario) {
        Reconocimiento r = new Reconocimiento();
        ReflectionTestUtils.setField(r, "id", id);
        r.setUsuario(destinatario);
        r.setTipo("BECA");
        r.setDescripcion("Beca de excelencia");
        r.setAnio(2025);
        return r;
    }

    public static ReconocimientoRequest createRequest() {
        return new ReconocimientoRequest("BECA", "Beca de excelencia", 2025);
    }
}
