package com.matchbar.repository;

import com.matchbar.entity.Incident;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface IncidentRepository extends MongoRepository<Incident, String> {
    List<Incident> findAllByOrderByCreatedAtDesc();
    List<Incident> findByStatusOrderByCreatedAtDesc(Incident.Status status);
}
