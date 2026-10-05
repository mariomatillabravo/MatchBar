package com.matchbar.config;

import com.matchbar.entity.User;
import com.matchbar.repository.UserRepository;
import com.matchbar.util.Emails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el primer administrador en entornos sin datos de prueba (todo salvo
 * "dev", donde lo hace {@link DataSeeder}). Las credenciales llegan por
 * MATCHBAR_ADMIN_EMAIL / MATCHBAR_ADMIN_PASSWORD y solo se usan si aún no
 * existe ningún ADMIN, así que tras el primer arranque pueden retirarse.
 */
@Slf4j
@Component
@Profile("!dev")
public class AdminBootstrap implements ApplicationRunner {

    static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${matchbar.bootstrap-admin.email:}") String email,
                          @Value("${matchbar.bootstrap-admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = Emails.normalize(email);
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(User.Role.ADMIN)) return;

        if (email.isEmpty() || password.isEmpty()) {
            log.warn("[AdminBootstrap] No existe ningún ADMIN. Define MATCHBAR_ADMIN_EMAIL y "
                    + "MATCHBAR_ADMIN_PASSWORD para crearlo; mientras tanto el panel de administración es inaccesible.");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("MATCHBAR_ADMIN_PASSWORD debe tener al menos "
                    + MIN_PASSWORD_LENGTH + " caracteres");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Ya existe un usuario con el email " + email
                    + " y no es ADMIN. Usa otro email en MATCHBAR_ADMIN_EMAIL.");
        }

        userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name("Administrador")
                .role(User.Role.ADMIN)
                .build());
        log.info("[AdminBootstrap] Creado el administrador inicial {}", email);
    }
}
