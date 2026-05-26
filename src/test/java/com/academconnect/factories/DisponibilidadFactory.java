package com.academconnect.factories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;

import com.academconnect.domain.DisponibilidadEvaluador;
import com.academconnect.domain.Usuario;
import com.academconnect.dto.DisponibilidadRequest;

public class DisponibilidadFactory {

    public static DisponibilidadEvaluador createDisponibilidad(Long id, Usuario evaluador, LocalDate fecha, BigDecimal horas) {
        DisponibilidadEvaluador d = new DisponibilidadEvaluador();
        ReflectionTestUtils.setField(d, "id", id);
        d.setEvaluador(evaluador);
        d.setFecha(fecha);
        d.setHorasDisponibles(horas);
        return d;
    }

    public static DisponibilidadRequest createRequest(LocalDate fecha, BigDecimal horas) {
        return new DisponibilidadRequest(List.of(new DisponibilidadRequest.Item(fecha, horas)));
    }
}
