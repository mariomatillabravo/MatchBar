package com.matchbar.service;

import com.matchbar.dto.response.MatchResponse;
import com.matchbar.entity.Competition;
import com.matchbar.entity.Match;
import com.matchbar.entity.Team;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.BarRepository;
import com.matchbar.repository.BroadcastRepository;
import com.matchbar.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock MatchRepository matchRepository;
    @Mock BroadcastRepository broadcastRepository;
    @Mock BarRepository barRepository;
    @Mock MongoTemplate mongoTemplate;
    @InjectMocks MatchService matchService;

    @Test
    void idDeCompeticionMalFormadoDevuelve400() {
        ApiException ex = assertThrows(ApiException.class,
                () -> matchService.search(null, null, "no-es-un-id", null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void idDeEquipoMalFormadoDevuelve400() {
        ApiException ex = assertThrows(ApiException.class,
                () -> matchService.search(null, null, null, "123"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void unPartidoSinEquiposNoRompeElListado() {
        Competition champions = Competition.builder().id("c1").name("Champions League").build();
        Match completo = Match.builder().id("m1").competition(champions)
                .homeTeam(Team.builder().id("t1").name("Real Madrid").build())
                .awayTeam(Team.builder().id("t2").name("Manchester City").build())
                .kickoffAt(Instant.now().plusSeconds(3600)).build();
        // Eliminatoria con rival por determinar: antes provocaba un NPE en MatchResponse.from.
        Match sinRival = Match.builder().id("m2").competition(champions)
                .homeTeam(Team.builder().id("t1").name("Real Madrid").build())
                .kickoffAt(Instant.now().plusSeconds(7200)).build();
        when(mongoTemplate.find(any(Query.class), eq(Match.class))).thenReturn(List.of(completo, sinRival));

        List<MatchResponse> result = matchService.search(null, null, null, null);

        assertEquals(1, result.size());
        assertEquals("m1", result.get(0).id());
    }
}
