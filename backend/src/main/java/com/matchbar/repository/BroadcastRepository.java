package com.matchbar.repository;

import com.matchbar.entity.Broadcast;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface BroadcastRepository extends MongoRepository<Broadcast, String> {
    List<Broadcast> findByBarId(String barId);
    List<Broadcast> findByMatchId(String matchId);
    boolean existsByBarIdAndMatchId(String barId, String matchId);
    void deleteByBarIdAndMatchId(String barId, String matchId);
}
