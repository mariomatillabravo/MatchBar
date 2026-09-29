package com.matchbar.config;

import com.matchbar.entity.User;
import com.matchbar.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    private static final String PASSWORD = "una-clave-larga-y-segura";

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @Test
    void creaElAdminCuandoNoHayNinguno() {
        when(userRepository.existsByRole(User.Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmail("admin@matchbar.es")).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("hash");

        new AdminBootstrap(userRepository, passwordEncoder, " admin@matchbar.es ", PASSWORD).run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("admin@matchbar.es", saved.getValue().getEmail());
        assertEquals("hash", saved.getValue().getPassword());
        assertEquals(User.Role.ADMIN, saved.getValue().getRole());
    }

    @Test
    void noHaceNadaSiYaExisteUnAdmin() {
        when(userRepository.existsByRole(User.Role.ADMIN)).thenReturn(true);

        new AdminBootstrap(userRepository, passwordEncoder, "admin@matchbar.es", PASSWORD).run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void sinCredencialesSoloAvisa() {
        when(userRepository.existsByRole(User.Role.ADMIN)).thenReturn(false);

        new AdminBootstrap(userRepository, passwordEncoder, "", "").run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void contrasenaCortaImpideArrancar() {
        when(userRepository.existsByRole(User.Role.ADMIN)).thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> new AdminBootstrap(userRepository, passwordEncoder, "admin@matchbar.es", "corta").run(null));
        verify(userRepository, never()).save(any());
    }
}
