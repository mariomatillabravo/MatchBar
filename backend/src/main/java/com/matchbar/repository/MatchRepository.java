package com.matchbar.repository;

import com.matchbar.entity.Match;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchRepository extends MongoRepository<Match, String> {}
