package com.academconnect.dto;

import com.academconnect.domain.PropositoToken;

public record VerificarTokenResponse(boolean valido, PropositoToken proposito) {
}
