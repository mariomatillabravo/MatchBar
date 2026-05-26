package com.matchbar.repository;

import com.matchbar.entity.Bar;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface BarRepository extends MongoRepository<Bar, String> {
    Optional<Bar> findByUserId(String userId);
    List<Bar> findByStatus(Bar.Status status);
    long countByStatus(Bar.Status status);
}
