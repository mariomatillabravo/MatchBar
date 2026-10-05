package com.matchbar.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchbar.exception.ErrorResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

/**
 * Límite de peticiones por IP en login y registro (credential stuffing,
 * creación masiva de cuentas). Es holgado a propósito: una clase entera
 * detrás de la misma wifi comparte IP. La protección fina por cuenta está en
 * {@link LoginAttemptGuard}.
 *
 * La IP es la del cliente real también detrás del proxy HTTPS gracias a
 * server.forward-headers-strategy (application.yml).
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";

    private final ObjectMapper objectMapper;
    private final FixedWindowRateLimiter loginLimiter;
    private final FixedWindowRateLimiter registerLimiter;

    public AuthRateLimitFilter(ObjectMapper objectMapper,
                               @Value("${matchbar.rate-limit.login-per-minute:30}") int loginPerMinute,
                               @Value("${matchbar.rate-limit.register-per-hour:20}") int registerPerHour) {
        this.objectMapper = objectMapper;
        this.loginLimiter = new FixedWindowRateLimiter(loginPerMinute, Duration.ofMinutes(1), Clock.systemUTC());
        this.registerLimiter = new FixedWindowRateLimiter(registerPerHour, Duration.ofHours(1), Clock.systemUTC());
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !HttpMethod.POST.name().equals(request.getMethod()) || limiterFor(request) == null;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        FixedWindowRateLimiter limiter = limiterFor(request);
        String clientIp = request.getRemoteAddr();
        if (!limiter.tryAcquire(clientIp)) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(limiter.retryAfter(clientIp).toSeconds()));
            ErrorResponses.write(response, objectMapper, HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiadas peticiones desde tu conexión. Inténtalo de nuevo en unos minutos.");
            return;
        }
        chain.doFilter(request, response);
    }

    private FixedWindowRateLimiter limiterFor(HttpServletRequest request) {
        return switch (request.getRequestURI()) {
            case LOGIN_PATH -> loginLimiter;
            case REGISTER_PATH -> registerLimiter;
            default -> null;
        };
    }
}
