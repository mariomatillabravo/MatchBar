package com.matchbar.service;

import com.matchbar.dto.request.UserAdminUpdateRequest;
import com.matchbar.entity.Bar;
import com.matchbar.entity.User;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.BarRepository;
import com.matchbar.repository.FavoriteRepository;
import com.matchbar.repository.ReviewRepository;
import com.matchbar.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock UserRepository userRepository;
    @Mock BarRepository barRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock FavoriteRepository favoriteRepository;
    @Mock BarService barService;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserAdminService service;

    private final User owner = User.builder().id("u-bar").email("rincon@test.com").role(User.Role.BAR).build();

    @Test
    void borrarUnUsuarioBorraTambienSuBarSusResenasYSusFavoritos() {
        Bar bar = Bar.builder().id("b1").userId("u-bar").build();
        when(userRepository.findById("u-bar")).thenReturn(Optional.of(owner));
        when(barRepository.findByUserId("u-bar")).thenReturn(Optional.of(bar));

        service.delete("u-bar", "admin");

        verify(barService).deleteWithDependents(bar);
        verify(reviewRepository).deleteByUserId("u-bar");
        verify(favoriteRepository).deleteByUserId("u-bar");
        verify(userRepository).delete(owner);
    }

    @Test
    void unAdminNoPuedeBorrarSuPropiaCuenta() {
        assertConflict(() -> service.delete("admin", "admin"));
        verify(userRepository, never()).delete(any());
    }

    @Test
    void unAdminNoPuedeQuitarseElRolDeAdministrador() {
        when(userRepository.findById("admin")).thenReturn(Optional.of(
                User.builder().id("admin").email("admin@matchbar.com").role(User.Role.ADMIN).build()));

        assertConflict(() -> service.update("admin",
                new UserAdminUpdateRequest("Admin", "admin@matchbar.com", User.Role.USER, null), "admin"));
    }

    @Test
    void noSePuedeQuitarElRolBarAQuienTieneUnBar() {
        when(userRepository.findById("u-bar")).thenReturn(Optional.of(owner));
        when(barRepository.findByUserId("u-bar")).thenReturn(Optional.of(Bar.builder().id("b1").build()));

        assertConflict(() -> service.update("u-bar",
                new UserAdminUpdateRequest("Rincón", "rincon@test.com", User.Role.USER, null), "admin"));
    }

    @Test
    void alEditarSeNormalizaElEmail() {
        User mario = User.builder().id("u1").email("mario@test.com").role(User.Role.USER).build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(mario));
        when(userRepository.existsByEmail("mario.nuevo@test.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String email = service.update("u1",
                new UserAdminUpdateRequest("Mario", " Mario.Nuevo@Test.com ", User.Role.USER, null), "admin").email();

        assertEquals("mario.nuevo@test.com", email);
    }

    private static void assertConflict(Runnable call) {
        ApiException ex = assertThrows(ApiException.class, call::run);
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }
}
