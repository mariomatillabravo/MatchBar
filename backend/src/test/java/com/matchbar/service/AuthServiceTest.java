package com.matchbar.service;

import com.matchbar.TestClock;
import com.matchbar.dto.request.LoginRequest;
import com.matchbar.dto.request.RegisterRequest;
import com.matchbar.entity.User;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.UserRepository;
import com.matchbar.security.JwtTokenProvider;
import com.matchbar.security.LoginAttemptGuard;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final int MAX_FAILURES = 3;

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // coste bajo: tests rápidos
    private final TestClock clock = new TestClock();
    private final AuthService service = new AuthService(userRepository, encoder,
            new JwtTokenProvider("x".repeat(32), 3_600_000),
            new LoginAttemptGuard(MAX_FAILURES, Duration.ofMinutes(15), clock));

    private final User mario = User.builder().id("u1").email("mario@test.com")
            .password(encoder.encode("password123")).name("Mario").role(User.Role.USER).build();

    @Test
    void elRegistroGuardaElEmailNormalizado() {
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.register(new RegisterRequest("  Mario@Test.COM ", "password123", "Mario", null));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("mario@test.com", saved.getValue().getEmail());
    }

    @Test
    void elRegistroDetectaDuplicadosSinImportarMayusculas() {
        when(userRepository.existsByEmail("mario@test.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.register(new RegisterRequest("MARIO@test.com", "password123", "Mario", null)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void elLoginEncuentraLaCuentaAunqueSeEscribaConMayusculas() {
        when(userRepository.findByEmail("mario@test.com")).thenReturn(Optional.of(mario));

        assertEquals("u1", service.login(new LoginRequest("Mario@Test.com", "password123")).userId());
    }

    @Test
    void trasVariosFallosLaCuentaSeBloqueaAunqueSeAcierteLaContrasena() {
        when(userRepository.findByEmail("mario@test.com")).thenReturn(Optional.of(mario));
        for (int i = 0; i < MAX_FAILURES; i++) {
            assertStatus(HttpStatus.UNAUTHORIZED, () -> service.login(new LoginRequest("mario@test.com", "mala")));
        }

        assertStatus(HttpStatus.TOO_MANY_REQUESTS,
                () -> service.login(new LoginRequest("mario@test.com", "password123")));
    }

    @Test
    void elBloqueoCaducaPasadoElTiempo() {
        when(userRepository.findByEmail("mario@test.com")).thenReturn(Optional.of(mario));
        for (int i = 0; i < MAX_FAILURES; i++) {
            assertStatus(HttpStatus.UNAUTHORIZED, () -> service.login(new LoginRequest("mario@test.com", "mala")));
        }
        clock.advance(Duration.ofMinutes(16));

        assertEquals("u1", service.login(new LoginRequest("mario@test.com", "password123")).userId());
    }

    @Test
    void unLoginCorrectoReiniciaElContadorDeFallos() {
        when(userRepository.findByEmail("mario@test.com")).thenReturn(Optional.of(mario));
        for (int i = 0; i < MAX_FAILURES - 1; i++) {
            assertStatus(HttpStatus.UNAUTHORIZED, () -> service.login(new LoginRequest("mario@test.com", "mala")));
        }
        service.login(new LoginRequest("mario@test.com", "password123"));

        // Tras el acierto vuelve a disponer de todos los intentos.
        assertStatus(HttpStatus.UNAUTHORIZED, () -> service.login(new LoginRequest("mario@test.com", "mala")));
        assertEquals("u1", service.login(new LoginRequest("mario@test.com", "password123")).userId());
    }

    @Test
    void unEmailInexistenteDaElMismoErrorQueUnaContrasenaIncorrecta() {
        when(userRepository.findByEmail("nadie@test.com")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("nadie@test.com", "password123")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Credenciales inválidas", ex.getMessage());
    }

    private static void assertStatus(HttpStatus expected, Runnable call) {
        ApiException ex = assertThrows(ApiException.class, call::run);
        assertEquals(expected, ex.getStatus());
    }
}
