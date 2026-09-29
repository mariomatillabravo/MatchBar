package com.matchbar.service;

import com.matchbar.config.FootballProperties;
import com.matchbar.dto.external.FootballCompetitionDto;
import com.matchbar.dto.external.FootballMatchDto;
import com.matchbar.dto.external.FootballTeamDto;
import com.matchbar.repository.CompetitionRepository;
import com.matchbar.repository.MatchRepository;
import com.matchbar.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FootballSyncServiceTest {

    @Mock FootballProperties props;
    @Mock CompetitionRepository competitionRepository;
    @Mock TeamRepository teamRepository;
    @Mock MatchRepository matchRepository;
    @Mock RestTemplate footballRestTemplate;
    @InjectMocks FootballSyncService service;

    private final FootballCompetitionDto champions = new FootballCompetitionDto(2001L, "UEFA Champions League", "CL", null);

    @Test
    void partidoConRivalPorDeterminarSeOmite() {
        FootballMatchDto tbd = new FootballMatchDto(1L, Instant.now(), "SCHEDULED",
                new FootballTeamDto(86L, "Real Madrid", null),
                new FootballTeamDto(null, null, null),
                champions);

        assertFalse(service.upsertMatch(tbd));
        verifyNoInteractions(competitionRepository, teamRepository, matchRepository);
    }

    @Test
    void partidoCompletoSeGuarda() {
        when(competitionRepository.findByExternalId("CL")).thenReturn(Optional.empty());
        when(competitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(teamRepository.findByExternalId(any())).thenReturn(Optional.empty());
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(matchRepository.findByExternalId("2")).thenReturn(Optional.empty());

        FootballMatchDto dto = new FootballMatchDto(2L, Instant.now(), "SCHEDULED",
                new FootballTeamDto(86L, "Real Madrid", null),
                new FootballTeamDto(65L, "Manchester City", null),
                champions);

        assertTrue(service.upsertMatch(dto));
        verify(matchRepository).save(any());
    }
}
