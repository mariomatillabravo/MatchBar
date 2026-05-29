package com.matchbar.repository;

import com.matchbar.entity.Competition;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface CompetitionRepository extends MongoRepository<Competition, String> {
    Optional<Competition> findByExternalId(String externalId);
}
