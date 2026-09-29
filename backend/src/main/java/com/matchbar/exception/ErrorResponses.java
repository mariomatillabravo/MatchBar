package com.matchbar.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Cuerpo JSON común de todos los errores de la API:
 * {@code {timestamp, status, error, message}}. Los clientes (app y panel)
 * muestran el campo {@code message}, así que debe ser legible para el usuario.
 */
public final class ErrorResponses {

    private ErrorResponses() {}

    public static Map<String, Object> body(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }

    public static ResponseEntity<Map<String, Object>> of(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(body(status, message));
    }
}
