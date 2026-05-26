package com.matchbar.repository;

import com.matchbar.entity.Broadcast;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BroadcastRepository extends JpaRepository<Broadcast, Long> {
    List<Broadcast> findByBarId(Long barId);
    Optional<Broadcast> findByBarIdAndMatchId(Long barId, Long matchId);
    boolean existsByBarIdAndMatchId(Long barId, Long matchId);
    void deleteByBarIdAndMatchId(Long barId, Long matchId);
}
