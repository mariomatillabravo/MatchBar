package com.matchbar.service;

import com.matchbar.dto.request.LoginRequest;
import com.matchbar.dto.request.RegisterRequest;
import com.matchbar.dto.response.AuthResponse;
import com.matchbar.entity.User;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.UserRepository;
import com.matchbar.security.JwtTokenProvider;
import com.matchbar.security.LoginAttemptGuard;
import com.matchbar.util.Emails;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final LoginAttemptGuard loginAttempts;
    /** Hash con el que se compara cuando el email no existe (ver login). */
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider, LoginAttemptGuard loginAttempts) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.loginAttempts = loginAttempts;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public AuthResponse register(RegisterRequest req) {
        String email = Emails.normalize(req.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un usuario con ese email");
        }
        User.Role role = req.role() != null ? req.role() : User.Role.USER;
        if (role == User.Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No se puede registrar como ADMIN");
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(req.password()))
                .name(req.name())
                .role(role)
                .build();
        user = userRepository.save(user);
        return toResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        String email = Emails.normalize(req.email());
        loginAttempts.checkNotBlocked(email);

        User user = userRepository.findByEmail(email).orElse(null);
        // Comparamos siempre contra un hash BCrypt, aunque el email no exista,
        // para que el tiempo de respuesta no revele qué emails están registrados.
        String hash = user != null ? user.getPassword() : dummyPasswordHash;
        boolean valid = passwordEncoder.matches(req.password(), hash) && user != null;
        if (!valid) {
            loginAttempts.onFailure(email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
        loginAttempts.onSuccess(email);
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        String token = tokenProvider.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getRole());
    }
}
