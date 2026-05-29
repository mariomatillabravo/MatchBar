package com.matchbar.repository;

import com.matchbar.entity.Team;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface TeamRepository extends MongoRepository<Team, String> {
    List<Team> findByCompetitionId(String competitionId);
    Optional<Team> findByExternalId(String externalId);
}
