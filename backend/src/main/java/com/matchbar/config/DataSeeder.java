package com.matchbar.config;

import com.matchbar.entity.*;
import com.matchbar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final BarRepository barRepository;
    private final CompetitionRepository competitionRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final BroadcastRepository broadcastRepository;
    private final ReviewRepository reviewRepository;
    private final IncidentRepository incidentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) return;

        String hash = passwordEncoder.encode("password123");

        // ── Usuarios ────────────────────────────────────────────────────────────
        User admin      = userRepository.save(User.builder().email("admin@matchbar.com").password(hash).name("Administrador").role(User.Role.ADMIN).build());
        User mario      = userRepository.save(User.builder().email("mario@test.com").password(hash).name("Mario Matilla").role(User.Role.USER).build());
        User lucia      = userRepository.save(User.builder().email("lucia@test.com").password(hash).name("Lucía Gómez").role(User.Role.USER).build());
        User carlos     = userRepository.save(User.builder().email("carlos@test.com").password(hash).name("Carlos Ruiz").role(User.Role.USER).build());

        User uRincon      = userRepository.save(User.builder().email("rincon@test.com").password(hash).name("El Rincón del Hincha").role(User.Role.BAR).build());
        User uCentenario  = userRepository.save(User.builder().email("centenario@test.com").password(hash).name("Bar Centenario").role(User.Role.BAR).build());
        User uPenalti     = userRepository.save(User.builder().email("penalti@test.com").password(hash).name("El Penalti").role(User.Role.BAR).build());
        User uMarcador    = userRepository.save(User.builder().email("marcador@test.com").password(hash).name("Bar El Marcador").role(User.Role.BAR).build());
        User uOffside     = userRepository.save(User.builder().email("offside@test.com").password(hash).name("Offside Sports Bar").role(User.Role.BAR).build());
        User uDerby       = userRepository.save(User.builder().email("derby@test.com").password(hash).name("Derby Bar").role(User.Role.BAR).build());
        User uCorner      = userRepository.save(User.builder().email("corner@test.com").password(hash).name("The Corner Pub").role(User.Role.BAR).build());
        User uUltra       = userRepository.save(User.builder().email("ultra@test.com").password(hash).name("Ultra Bar").role(User.Role.BAR).build());

        // ── Bares APROBADOS ─────────────────────────────────────────────────────
        Bar rincon = barRepository.save(Bar.builder()
                .userId(uRincon.getId()).name("El Rincón del Hincha")
                .description("Bar tradicional con pantallas en cada esquina y ambiente futbolero. Especialidad en croquetas y cañas.")
                .address("Calle Bravo Murillo 56, Madrid")
                .latitude(new BigDecimal("40.4500")).longitude(new BigDecimal("-3.7038"))
                .location(new org.springframework.data.mongodb.core.geo.GeoJsonPoint(-3.7038, 40.4500))
                .licenseDoc("licencia-rincon.pdf").status(Bar.Status.APPROVED).build());

        Bar centenario = barRepository.save(Bar.builder()
                .userId(uCentenario.getId()).name("Bar Centenario")
                .description("Cerveza fría y fútbol nacional e internacional. Tapas caseras desde 1985.")
                .address("Calle de Alcalá 102, Madrid")
                .latitude(new BigDecimal("40.4220")).longitude(new BigDecimal("-3.6786"))
                .location(new org.springframework.data.mongodb.core.geo.GeoJsonPoint(-3.6786, 40.4220))
                .licenseDoc("licencia-centenario.pdf").status(Bar.Status.APPROVED).build());

        // ── Bares PENDIENTES (visibles en el panel admin para aprobar/rechazar) ─
        barRepository.save(Bar.builder()
                .userId(uPenalti.getId()).name("El Penalti")
                .description("Pantalla gigante 4K. Reservado para grupos los días de Champions.")
                .address("Plaza Mayor 8, Madrid")
                .latitude(new BigDecimal("40.4155")).longitude(new BigDecimal("-3.7074"))
                .location(new org.springframework.data.mongodb.core.geo.GeoJsonPoint(-3.7074, 40.4155))
                .licenseDoc("licencia-penalti.pdf").status(Bar.Status.PENDING).build());

        barRepository.save(Bar.builder()
                .userId(uMarcador.getId()).name("Bar El Marcador")
                .description("Dos plantas, 8 pantallas y carta de cervezas artesanas. Ambiente familiar.")
                .address("Calle Fuencarral 143, Madrid")
                .latitude(new BigDecimal("40.4310")).longitude(new BigDecimal("-3.7020"))
                .location(new org.springframework.data.mongodb.core.geo.GeoJsonPoint(-3.7020, 40.4310))
                .licenseDoc("licencia-marcador.pdf").status(Bar.Status.PENDING).build());

        barRepository.save(Bar.builder()
                .userId(uOffside.getId()).name("Offside Sports Bar")
                .description("Sports bar al estilo anglosajón. Retransmisión de Premier, NFL y NBA.")
                .address("Calle Gran Vía 45, Madrid")
                .latitude(new BigDecimal("40.4200")).longitude(new BigDecimal("-3.7050"))
                .location(new org.springframework.data.mongodb.core.geo.GeoJsonPoint(-3.7050, 40.4200))
                .licenseDoc("licencia-offside.pdf").status(Bar.Status.PENDING).build());

        barRepository.save(Bar.builder()
                .userId(uDerby.getId()).name("Derby Bar")
                .description("El punto de encuentro de los aficionados del barrio. Terraza exterior en verano.")
                .address("Calle de Lavapiés 22, Madrid")
                .latitude(new BigDecimal("40.4090")).longitude(new BigDecimal("-3.7010"))
                .location(new org.springframework.data.mongodb.core.geo.GeoJsonPoint(-3.7010, 40.4090))
                .licenseDoc("licencia-derby.pdf").status(Bar.Status.PENDING).build());

        // ── Bares RECHAZADOS (para que aparezcan en las estadísticas) ───────────
        barRepository.save(Bar.builder()
                .userId(uCorner.getId()).name("The Corner Pub")
                .description("Pub inglés con retransmisiones de fútbol.")
                .address("Calle Serrano 12, Madrid")
                .latitude(new BigDecimal("40.4260")).longitude(new BigDecimal("-3.6880"))
                .location(new org.springframework.data.mongodb.core.geo.GeoJsonPoint(-3.6880, 40.4260))
                .licenseDoc("licencia-corner.pdf").status(Bar.Status.REJECTED).build());

        barRepository.save(Bar.builder()
                .userId(uUltra.getId()).name("Ultra Bar")
                .description("Bar de aficionados con decoración de equipos históricos.")
                .address("Avenida de América 3, Madrid")
                .latitude(new BigDecimal("40.4370")).longitude(new BigDecimal("-3.6760"))
                .location(new org.springframework.data.mongodb.core.geo.GeoJsonPoint(-3.6760, 40.4370))
                .licenseDoc("licencia-ultra.pdf").status(Bar.Status.REJECTED).build());

        // ── Competiciones y equipos ─────────────────────────────────────────────
        Competition laliga    = competitionRepository.save(Competition.builder().name("LaLiga").country("España").build());
        Competition premier   = competitionRepository.save(Competition.builder().name("Premier League").country("Inglaterra").build());
        Competition champions = competitionRepository.save(Competition.builder().name("Champions League").country("Europa").build());

        Team madrid   = teamRepository.save(Team.builder().name("Real Madrid").competitionId(laliga.getId()).build());
        Team barca    = teamRepository.save(Team.builder().name("FC Barcelona").competitionId(laliga.getId()).build());
        Team atletico = teamRepository.save(Team.builder().name("Atlético de Madrid").competitionId(laliga.getId()).build());
        Team sevilla  = teamRepository.save(Team.builder().name("Sevilla FC").competitionId(laliga.getId()).build());
        Team city     = teamRepository.save(Team.builder().name("Manchester City").competitionId(premier.getId()).build());
        Team liverpool = teamRepository.save(Team.builder().name("Liverpool FC").competitionId(premier.getId()).build());
        teamRepository.save(Team.builder().name("Arsenal FC").competitionId(premier.getId()).build());

        // ── Partidos ────────────────────────────────────────────────────────────
        Instant now = Instant.now();
        Match m1 = matchRepository.save(Match.builder().competition(laliga).homeTeam(madrid).awayTeam(barca).kickoffAt(now.plus(1, ChronoUnit.DAYS)).build());
        Match m2 = matchRepository.save(Match.builder().competition(laliga).homeTeam(atletico).awayTeam(sevilla).kickoffAt(now.plus(2, ChronoUnit.DAYS)).build());
        Match m3 = matchRepository.save(Match.builder().competition(premier).homeTeam(city).awayTeam(liverpool).kickoffAt(now.plus(3, ChronoUnit.DAYS)).build());
        Match m4 = matchRepository.save(Match.builder().competition(champions).homeTeam(madrid).awayTeam(city).kickoffAt(now.plus(5, ChronoUnit.DAYS)).build());
        Match m5 = matchRepository.save(Match.builder().competition(laliga).homeTeam(barca).awayTeam(atletico).kickoffAt(now.plus(7, ChronoUnit.DAYS)).build());

        // ── Retransmisiones ─────────────────────────────────────────────────────
        broadcastRepository.saveAll(List.of(
                Broadcast.builder().barId(rincon.getId()).matchId(m1.getId()).build(),
                Broadcast.builder().barId(centenario.getId()).matchId(m1.getId()).build(),
                Broadcast.builder().barId(rincon.getId()).matchId(m4.getId()).build(),
                Broadcast.builder().barId(centenario.getId()).matchId(m3.getId()).build(),
                Broadcast.builder().barId(rincon.getId()).matchId(m5.getId()).build()
        ));

        // ── Reseñas ─────────────────────────────────────────────────────────────
        reviewRepository.saveAll(List.of(
                Review.builder().userId(mario.getId()).userName(mario.getName()).barId(rincon.getId())
                        .ratingAtmosphere(5).ratingFood(4).ratingPrice(3)
                        .comment("Ambiente brutal en los partidos del Madrid. Se ven todas las pantallas bien.").build(),
                Review.builder().userId(mario.getId()).userName(mario.getName()).barId(centenario.getId())
                        .ratingAtmosphere(4).ratingFood(5).ratingPrice(4)
                        .comment("Las tapas están muy buenas y el precio es razonable.").build(),
                Review.builder().userId(lucia.getId()).userName(lucia.getName()).barId(rincon.getId())
                        .ratingAtmosphere(5).ratingFood(3).ratingPrice(4)
                        .comment("Muy buen ambiente. Algo caro para lo que ofrecen de comer.").build(),
                Review.builder().userId(carlos.getId()).userName(carlos.getName()).barId(centenario.getId())
                        .ratingAtmosphere(3).ratingFood(5).ratingPrice(5)
                        .comment("La mejor tortilla de patatas del barrio. El sitio es pequeño pero acogedor.").build()
        ));

        // ── Incidencias (pestaña Incidencias del panel admin) ───────────────────
        Instant twoDaysAgo  = now.minus(2, ChronoUnit.DAYS);
        Instant fourDaysAgo = now.minus(4, ChronoUnit.DAYS);
        Instant sixDaysAgo  = now.minus(6, ChronoUnit.DAYS);

        incidentRepository.saveAll(List.of(
                // Abiertas
                Incident.builder()
                        .userId(uPenalti.getId()).senderName("El Penalti").senderEmail("penalti@test.com")
                        .senderType(User.Role.BAR)
                        .subject("No puedo subir el logo de mi bar")
                        .message("Llevo varios días intentando subir el logo pero la aplicación no me deja. ¿Hay algún problema con la subida de imágenes?")
                        .status(Incident.Status.OPEN).createdAt(twoDaysAgo).build(),

                Incident.builder()
                        .userId(mario.getId()).senderName("Mario Matilla").senderEmail("mario@test.com")
                        .senderType(User.Role.USER)
                        .subject("Bar cerrado pero aparece como abierto")
                        .message("El bar 'El Rincón del Hincha' aparece como que emite el partido del sábado pero estuve allí y estaba cerrado por obras.")
                        .status(Incident.Status.OPEN).createdAt(twoDaysAgo.minus(3, ChronoUnit.HOURS)).build(),

                Incident.builder()
                        .userId(uMarcador.getId()).senderName("Bar El Marcador").senderEmail("marcador@test.com")
                        .senderType(User.Role.BAR)
                        .subject("Mi solicitud lleva semanas sin revisarse")
                        .message("Envié la solicitud de alta hace 3 semanas y sigue en estado pendiente. Necesito que se revise cuanto antes para poder operar.")
                        .status(Incident.Status.OPEN).createdAt(fourDaysAgo).build(),

                Incident.builder()
                        .userId(lucia.getId()).senderName("Lucía Gómez").senderEmail("lucia@test.com")
                        .senderType(User.Role.USER)
                        .subject("Error al marcar favorito")
                        .message("Al intentar añadir un bar a favoritos me aparece un error en pantalla y no se guarda.")
                        .status(Incident.Status.OPEN).createdAt(now.minus(5, ChronoUnit.HOURS)).build(),

                // Resueltas
                Incident.builder()
                        .userId(carlos.getId()).senderName("Carlos Ruiz").senderEmail("carlos@test.com")
                        .senderType(User.Role.USER)
                        .subject("No puedo iniciar sesión")
                        .message("Me sale error 401 al intentar iniciar sesión con mis credenciales correctas.")
                        .status(Incident.Status.RESOLVED).createdAt(sixDaysAgo)
                        .resolvedAt(sixDaysAgo.plus(6, ChronoUnit.HOURS)).build(),

                Incident.builder()
                        .userId(uOffside.getId()).senderName("Offside Sports Bar").senderEmail("offside@test.com")
                        .senderType(User.Role.BAR)
                        .subject("Dirección incorrecta en mi ficha")
                        .message("La dirección que aparece en mi ficha no es correcta. La correcta es Calle Gran Vía 45.")
                        .status(Incident.Status.RESOLVED).createdAt(fourDaysAgo.minus(1, ChronoUnit.DAYS))
                        .resolvedAt(fourDaysAgo).build()
        ));
    }
}
