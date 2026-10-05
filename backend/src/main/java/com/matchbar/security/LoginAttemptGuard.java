package com.matchbar.security;

import com.matchbar.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/**
 * Bloquea temporalmente una cuenta tras varios intentos de login fallidos,
 * para frenar ataques de fuerza bruta contra una contraseña concreta. Cuenta
 * por email (también los que no existen, para no revelar cuáles están
 * registrados) y se reinicia con un login correcto.
 */
@Component
public class LoginAttemptGuard {

    private final FixedWindowRateLimiter failures;

    @Autowired
    public LoginAttemptGuard(@Value("${matchbar.rate-limit.login-failures-per-account:5}") int maxFailures,
                             @Value("${matchbar.rate-limit.login-lockout-minutes:15}") long lockoutMinutes) {
        this(maxFailures, Duration.ofMinutes(lockoutMinutes), Clock.systemUTC());
    }

    /** Constructor explícito, útil en tests para controlar el reloj. */
    public LoginAttemptGuard(int maxFailures, Duration lockout, Clock clock) {
        this.failures = new FixedWindowRateLimiter(maxFailures, lockout, clock);
    }

    public void checkNotBlocked(String email) {
        if (failures.isLimited(email)) {
            long minutes = Math.max(1, (failures.retryAfter(email).toSeconds() + 59) / 60);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Vuelve a intentarlo en " + minutes + " min.");
        }
    }

    public void onFailure(String email) {
        failures.tryAcquire(email);
    }

    public void onSuccess(String email) {
        failures.reset(email);
    }
}
