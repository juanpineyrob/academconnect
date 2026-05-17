package com.academconnect.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s con id %s no encontrado".formatted(resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
