package com.academconnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RateLimiterServiceTests {

    @Test
    void permiteHastaElLimiteYLuegoBloquea() {
        var rl = new RateLimiterService();
        for (int i = 0; i < 3; i++) {
            assertThat(rl.permitir("k", 3, Duration.ofMinutes(1))).isTrue();
        }
        assertThat(rl.permitir("k", 3, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void clavesDistintasNoInterfieren() {
        var rl = new RateLimiterService();
        assertThat(rl.permitir("a", 1, Duration.ofMinutes(1))).isTrue();
        assertThat(rl.permitir("b", 1, Duration.ofMinutes(1))).isTrue();
    }
}
