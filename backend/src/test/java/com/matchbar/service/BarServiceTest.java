package com.matchbar.service;

import com.matchbar.dto.request.BarUpsertRequest;
import com.matchbar.entity.Bar;
import com.matchbar.entity.User;
import com.matchbar.repository.BarRepository;
import com.matchbar.repository.BroadcastRepository;
import com.matchbar.repository.ReviewRepository;
import com.matchbar.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BarServiceTest {

    @Mock BarRepository barRepository;
    @Mock UserRepository userRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock BroadcastRepository broadcastRepository;
    @Mock MongoTemplate mongoTemplate;
    @Mock GeocodingService geocodingService;
    @InjectMocks BarService barService;

    private Bar bar;

    @BeforeEach
    void setUp() {
        when(userRepository.findById("owner")).thenReturn(Optional.of(
                User.builder().id("owner").role(User.Role.BAR).build()));
        bar = Bar.builder().id("b1").userId("owner").name("El Rincón del Hincha")
                .description("Bar de fútbol").address("Calle Bravo Murillo 56, Madrid")
                .location(new GeoJsonPoint(-3.70, 40.45)).build();
        when(barRepository.findByUserId("owner")).thenReturn(Optional.of(bar));
        when(barRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(geocodingService.geocode(any())).thenReturn(new GeocodingService.Coordinates(40.41, -3.70));
    }

    @Test
    void unBarAprobadoQueCambiaDeNombreVuelveARevision() {
        bar.setStatus(Bar.Status.APPROVED);

        save("Bar Nuevo", "Calle Bravo Murillo 56, Madrid", "Bar de fútbol");

        assertEquals(Bar.Status.PENDING, bar.getStatus());
    }

    @Test
    void unBarAprobadoQueCambiaDeDireccionVuelveARevision() {
        bar.setStatus(Bar.Status.APPROVED);

        save("El Rincón del Hincha", "Plaza Mayor 1, Madrid", "Bar de fútbol");

        assertEquals(Bar.Status.PENDING, bar.getStatus());
    }

    @Test
    void cambiarSoloLaDescripcionNoRequiereRevision() {
        bar.setStatus(Bar.Status.APPROVED);

        save("El Rincón del Hincha", "Calle Bravo Murillo 56, Madrid", "Ahora con terraza");

        assertEquals(Bar.Status.APPROVED, bar.getStatus());
    }

    @Test
    void cambiosDeMayusculasOEspaciosNoCuentanComoCambio() {
        bar.setStatus(Bar.Status.APPROVED);

        save(" el rincón del hincha ", "calle bravo murillo 56, madrid", "Bar de fútbol");

        assertEquals(Bar.Status.APPROVED, bar.getStatus());
    }

    @Test
    void unBarRechazadoQueEditaSuFichaSeReenviaARevision() {
        bar.setStatus(Bar.Status.REJECTED);

        save("El Rincón del Hincha", "Calle Bravo Murillo 56, Madrid", "Hemos corregido la licencia");

        assertEquals(Bar.Status.PENDING, bar.getStatus());
    }

    private void save(String name, String address, String description) {
        barService.createOrUpdateForUser("owner", new BarUpsertRequest(name, description, address, null));
    }
}
