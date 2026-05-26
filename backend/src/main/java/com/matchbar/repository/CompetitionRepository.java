package com.matchbar.repository;

import com.matchbar.entity.Competition;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompetitionRepository extends MongoRepository<Competition, String> {}
