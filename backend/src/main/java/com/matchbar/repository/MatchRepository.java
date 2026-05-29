package com.matchbar.repository;

import com.matchbar.entity.Match;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface MatchRepository extends MongoRepository<Match, String> {
    Optional<Match> findByExternalId(String externalId);
}
