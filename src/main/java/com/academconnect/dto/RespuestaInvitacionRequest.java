package com.academconnect.dto;

import jakarta.validation.constraints.Size;

public record RespuestaInvitacionRequest(@Size(max = 1000) String respuesta) {
}
