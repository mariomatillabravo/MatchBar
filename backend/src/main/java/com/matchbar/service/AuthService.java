package com.matchbar.service;

import com.matchbar.dto.request.LoginRequest;
import com.matchbar.dto.request.RegisterRequest;
import com.matchbar.dto.response.AuthResponse;
import com.matchbar.entity.User;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.UserRepository;
import com.matchbar.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un usuario con ese email");
        }
        User.Role role = req.role() != null ? req.role() : User.Role.USER;
        if (role == User.Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No se puede registrar como ADMIN");
        }
        User user = User.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .name(req.name())
                .role(role)
                .build();
        user = userRepository.save(user);
        return toResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        String token = tokenProvider.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getRole());
    }
}
