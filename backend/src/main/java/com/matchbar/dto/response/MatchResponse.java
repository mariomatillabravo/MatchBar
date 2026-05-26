package com.matchbar.dto.response;

import com.matchbar.entity.Match;
import java.time.Instant;

public record MatchResponse(
        Long id,
        Long competitionId,
        String competitionName,
        Long homeTeamId,
        String homeTeamName,
        String homeTeamLogo,
        Long awayTeamId,
        String awayTeamName,
        String awayTeamLogo,
        Instant kickoffAt
) {
    public static MatchResponse from(Match m) {
        return new MatchResponse(
                m.getId(),
                m.getCompetition().getId(),
                m.getCompetition().getName(),
                m.getHomeTeam().getId(),
                m.getHomeTeam().getName(),
                m.getHomeTeam().getLogoUrl(),
                m.getAwayTeam().getId(),
                m.getAwayTeam().getName(),
                m.getAwayTeam().getLogoUrl(),
                m.getKickoffAt()
        );
    }
}
