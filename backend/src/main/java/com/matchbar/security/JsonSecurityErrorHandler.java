package com.matchbar.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchbar.exception.ErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Respuestas de Spring Security en el mismo formato JSON que el resto de la API.
 * Distingue 401 (sin token, token inválido o caducado: el cliente debe volver a
 * iniciar sesión) de 403 (sesión válida pero sin el rol necesario).
 */
@Component
@RequiredArgsConstructor
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        ErrorResponses.write(response, objectMapper, HttpStatus.UNAUTHORIZED, "Sesión no iniciada o caducada");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        ErrorResponses.write(response, objectMapper, HttpStatus.FORBIDDEN, "No tienes permisos para esta acción");
    }
}
