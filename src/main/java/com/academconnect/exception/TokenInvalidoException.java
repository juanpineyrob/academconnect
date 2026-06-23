package com.academconnect.exception;

/** Token de activación/reset inexistente, expirado, ya usado o de propósito incorrecto. Mapea a 400. */
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException() {
        super("El enlace es inválido o expiró. Solicitá uno nuevo.");
    }
}
