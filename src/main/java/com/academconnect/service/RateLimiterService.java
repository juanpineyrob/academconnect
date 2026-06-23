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
        if (q.size() >= maximo) {
            return false;
        }
        q.addLast(ahora);
        return true;
    }
}
