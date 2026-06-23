package com.academconnect.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Rate limiter in-memory por clave (sliding window). Suficiente para single-instance (prototipo);
 * para multi-instancia migrar a un store compartido (Redis/Bucket4j).
 */
@Service
public class RateLimiterService {

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    public synchronized boolean permitir(String clave, int maximo, Duration ventana) {
        Instant ahora = Instant.now();
        Instant limite = ahora.minus(ventana);
        Deque<Instant> q = hits.computeIfAbsent(clave, k -> new ArrayDeque<>());
        while (!q.isEmpty() && q.peekFirst().isBefore(limite)) {
            q.pollFirst();
        }
        // Oportunidad de purga: barre claves vecinas cuyos timestamps ya expiraron, evitando que el map
        // crezca de forma ilimitada por claves (email+IP) que no vuelven a usarse. La clave actual se
        // recrea más abajo si corresponde, así que es seguro evaluarla también.
        hits.values().removeIf(d -> {
            while (!d.isEmpty() && d.peekFirst().isBefore(limite)) {
                d.pollFirst();
            }
            return d.isEmpty();
        });
        if (q.size() >= maximo) {
            return false;
        }
        if (q.isEmpty()) {
            // La purga anterior pudo haber removido la clave actual al quedar vacía: reinsertarla.
            hits.put(clave, q);
        }
        q.addLast(ahora);
        return true;
    }
}
