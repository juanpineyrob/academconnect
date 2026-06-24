package com.academconnect.dto;

import com.academconnect.domain.ResultadoFila;

public record ImportItemResponse(int linea, String matricula, String email, String nombre,
        ResultadoFila resultado, String detalle) {
}
