package com.matchbar.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FootballMatchDto(
        Long id,
        Instant utcDate,
        String status,
        FootballTeamDto homeTeam,
        FootballTeamDto awayTeam,
        FootballCompetitionDto competition
) {}
