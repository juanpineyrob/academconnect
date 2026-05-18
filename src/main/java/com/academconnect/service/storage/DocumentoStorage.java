package com.academconnect.service.storage;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentoStorage {

    /** Almacena el contenido y devuelve la storage key relativa. */
    String store(InputStream content, String nombreOriginal, String contentType) throws IOException;

    /** Devuelve un stream con el contenido. El caller debe cerrarlo. */
    InputStream retrieve(String storageKey) throws IOException;

    /** Elimina el documento. Idempotente. */
    void delete(String storageKey) throws IOException;

    boolean exists(String storageKey);
}
