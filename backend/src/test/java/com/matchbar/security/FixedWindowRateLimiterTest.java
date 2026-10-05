package com.matchbar.security;

import com.matchbar.TestClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedWindowRateLimiterTest {

    private final TestClock clock = new TestClock();
    private final FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(3, Duration.ofMinutes(1), clock);

    @Test
    void permiteHastaElLimiteYBloqueaDespues() {
        assertTrue(limiter.tryAcquire("1.2.3.4"));
        assertTrue(limiter.tryAcquire("1.2.3.4"));
        assertTrue(limiter.tryAcquire("1.2.3.4"));
        assertFalse(limiter.tryAcquire("1.2.3.4"));
    }

    @Test
    void cadaClaveTieneSuPropioCupo() {
        for (int i = 0; i < 3; i++) limiter.tryAcquire("1.2.3.4");

        assertTrue(limiter.tryAcquire("5.6.7.8"));
    }

    @Test
    void laVentanaSeReiniciaAlCaducar() {
        for (int i = 0; i < 4; i++) limiter.tryAcquire("1.2.3.4");
        clock.advance(Duration.ofSeconds(61));

        assertTrue(limiter.tryAcquire("1.2.3.4"));
    }

    @Test
    void isLimitedNoConsumeCupo() {
        limiter.tryAcquire("k");
        limiter.tryAcquire("k");
        assertFalse(limiter.isLimited("k"));
        limiter.tryAcquire("k");

        assertTrue(limiter.isLimited("k"));
    }

    @Test
    void retryAfterIndicaElTiempoRestante() {
        limiter.tryAcquire("k");
        clock.advance(Duration.ofSeconds(20));

        assertEquals(Duration.ofSeconds(40), limiter.retryAfter("k"));
    }

    @Test
    void resetLiberaLaClave() {
        for (int i = 0; i < 3; i++) limiter.tryAcquire("k");
        limiter.reset("k");

        assertFalse(limiter.isLimited("k"));
    }
}
