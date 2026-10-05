package com.matchbar.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limitador en memoria por clave (IP, email...) con ventanas fijas: como
 * máximo {@code maxEvents} eventos por {@code window}.
 *
 * Vive en la memoria de cada instancia: con varias réplicas de la API cada una
 * lleva su propia cuenta. Para escalar horizontalmente habría que moverlo a un
 * almacén compartido (Redis) o limitar en el proxy.
 */
public class FixedWindowRateLimiter {

    private static final int CLEANUP_EVERY = 1024;

    private record Window(Instant start, int count) {}

    private final int maxEvents;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicInteger callsSinceCleanup = new AtomicInteger();

    public FixedWindowRateLimiter(int maxEvents, Duration window, Clock clock) {
        this.maxEvents = maxEvents;
        this.window = window;
        this.clock = clock;
    }

    /** Registra un evento. Devuelve false si con él se supera el límite. */
    public boolean tryAcquire(String key) {
        Instant now = clock.instant();
        Window w = windows.compute(key, (k, old) -> old == null || isExpired(old, now)
                ? new Window(now, 1)
                : new Window(old.start(), old.count() + 1));
        cleanupIfNeeded(now);
        return w.count() <= maxEvents;
    }

    /** True si la clave ya ha agotado su cupo en la ventana actual (sin consumir). */
    public boolean isLimited(String key) {
        Window w = windows.get(key);
        return w != null && !isExpired(w, clock.instant()) && w.count() >= maxEvents;
    }

    public void reset(String key) {
        windows.remove(key);
    }

    /** Tiempo hasta que la ventana de la clave se reinicia (al menos 1 s). */
    public Duration retryAfter(String key) {
        Window w = windows.get(key);
        if (w == null) return Duration.ZERO;
        Duration remaining = Duration.between(clock.instant(), w.start().plus(window));
        return remaining.compareTo(Duration.ofSeconds(1)) < 0 ? Duration.ofSeconds(1) : remaining;
    }

    private boolean isExpired(Window w, Instant now) {
        return !now.isBefore(w.start().plus(window));
    }

    /** Evita que el mapa crezca sin límite con claves que ya no se usan. */
    private void cleanupIfNeeded(Instant now) {
        if (callsSinceCleanup.incrementAndGet() >= CLEANUP_EVERY) {
            callsSinceCleanup.set(0);
            windows.entrySet().removeIf(e -> isExpired(e.getValue(), now));
        }
    }
}
