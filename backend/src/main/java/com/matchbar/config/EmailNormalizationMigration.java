package com.matchbar.config;

import com.matchbar.entity.User;
import com.matchbar.repository.UserRepository;
import com.matchbar.util.Emails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Pasa a minúsculas los emails guardados antes de que se normalizaran (ver
 * {@link Emails}); sin esto esas cuentas no podrían iniciar sesión. Es
 * idempotente: en cada arranque solo toca los emails que aún tengan mayúsculas.
 * Se ejecuta antes que el resto de inicializaciones (AdminBootstrap...).
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class EmailNormalizationMigration implements ApplicationRunner {

    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        for (User user : userRepository.findByEmailRegex("[A-Z]|^\\s|\\s$")) {
            String normalized = Emails.normalize(user.getEmail());
            if (userRepository.existsByEmail(normalized)) {
                // Dos cuentas que solo se diferencian en mayúsculas: decide el admin.
                log.warn("[EmailNormalization] No se normaliza {} (id {}): ya existe una cuenta con {}",
                        user.getEmail(), user.getId(), normalized);
                continue;
            }
            user.setEmail(normalized);
            userRepository.save(user);
            log.info("[EmailNormalization] Email normalizado para el usuario {}", user.getId());
        }
    }
}
