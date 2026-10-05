package com.matchbar.config;

import com.matchbar.entity.User;
import com.matchbar.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNormalizationMigrationTest {

    private final UserRepository userRepository = mock(UserRepository.class);

    @Test
    void normalizaLosEmailsConMayusculasYRespetaLosConflictos() {
        User mario = User.builder().id("u1").email("Mario@Test.com").build();
        User luciaDuplicada = User.builder().id("u2").email("LUCIA@test.com").build();
        when(userRepository.findByEmailRegex(any())).thenReturn(List.of(mario, luciaDuplicada));
        when(userRepository.existsByEmail("mario@test.com")).thenReturn(false);
        when(userRepository.existsByEmail("lucia@test.com")).thenReturn(true); // ya hay otra cuenta

        new EmailNormalizationMigration(userRepository).run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(saved.capture());
        assertEquals("u1", saved.getValue().getId());
        assertEquals("mario@test.com", saved.getValue().getEmail());
    }
}
