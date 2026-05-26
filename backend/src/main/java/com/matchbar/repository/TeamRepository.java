package com.matchbar.repository;

import com.matchbar.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByCompetitionId(Long competitionId);
}
