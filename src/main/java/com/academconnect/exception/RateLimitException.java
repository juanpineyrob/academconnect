package com.academconnect.exception;

/** Se excedió el límite de solicitudes para una clave (IP / email). Mapea a HTTP 429. */
public class RateLimitException extends RuntimeException {

    public RateLimitException() {
        super("Demasiadas solicitudes. Intentá de nuevo más tarde.");
    }
}
