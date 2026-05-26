package com.matchbar.repository;

import com.matchbar.entity.Team;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TeamRepository extends MongoRepository<Team, String> {
    List<Team> findByCompetitionId(String competitionId);
}
