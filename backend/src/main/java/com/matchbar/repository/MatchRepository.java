package com.matchbar.repository;

import com.matchbar.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    @Query("""
        SELECT m FROM Match m
        WHERE (:from IS NULL OR m.kickoffAt >= :from)
          AND (:to IS NULL OR m.kickoffAt <= :to)
          AND (:competitionId IS NULL OR m.competition.id = :competitionId)
          AND (:teamId IS NULL OR m.homeTeam.id = :teamId OR m.awayTeam.id = :teamId)
        ORDER BY m.kickoffAt ASC
        """)
    List<Match> search(@Param("from") Instant from,
                       @Param("to") Instant to,
                       @Param("competitionId") Long competitionId,
                       @Param("teamId") Long teamId);
}
