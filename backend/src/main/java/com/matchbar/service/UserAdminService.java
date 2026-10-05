package com.matchbar.service;

import com.matchbar.dto.request.UserAdminUpdateRequest;
import com.matchbar.dto.response.UserAdminResponse;
import com.matchbar.entity.User;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.BarRepository;
import com.matchbar.repository.FavoriteRepository;
import com.matchbar.repository.ReviewRepository;
import com.matchbar.repository.UserRepository;
import com.matchbar.util.Emails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** Gestión de usuarios desde el panel de administración. */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final BarRepository barRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final BarService barService;
    private final PasswordEncoder passwordEncoder;

    /** Usuarios y administradores; los bares se gestionan en su propia pestaña. */
    public List<UserAdminResponse> listUsers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != User.Role.BAR)
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(UserAdminService::toResponse)
                .toList();
    }

    public UserAdminResponse get(String id) {
        return toResponse(find(id));
    }

    public UserAdminResponse update(String id, UserAdminUpdateRequest req, String currentAdminId) {
        User user = find(id);
        if (id.equals(currentAdminId) && req.role() != User.Role.ADMIN) {
            throw new ApiException(HttpStatus.CONFLICT, "No puedes quitarte el rol de administrador");
        }
        if (user.getRole() == User.Role.BAR && req.role() != User.Role.BAR && barRepository.findByUserId(id).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Este usuario tiene un bar. Elimina el bar antes de cambiarle el rol");
        }
        String email = Emails.normalize(req.email());
        if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un usuario con ese email");
        }
        user.setName(req.name());
        user.setEmail(email);
        user.setRole(req.role());
        if (req.password() != null && !req.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.password()));
        }
        return toResponse(userRepository.save(user));
    }

    /**
     * Borra la cuenta y lo que solo tiene sentido con ella: su bar (con sus
     * fotos, reseñas, favoritos y emisiones), sus reseñas y sus favoritos.
     * Las incidencias que envió se conservan como registro para el admin.
     */
    public void delete(String id, String currentAdminId) {
        if (id.equals(currentAdminId)) {
            throw new ApiException(HttpStatus.CONFLICT, "No puedes eliminar tu propia cuenta");
        }
        User user = find(id);
        barRepository.findByUserId(user.getId()).ifPresent(barService::deleteWithDependents);
        reviewRepository.deleteByUserId(user.getId());
        favoriteRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
    }

    private User find(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private static UserAdminResponse toResponse(User u) {
        return new UserAdminResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getCreatedAt());
    }
}
