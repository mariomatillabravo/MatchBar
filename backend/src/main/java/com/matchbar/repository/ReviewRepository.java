package com.matchbar.repository;

import com.matchbar.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBarIdOrderByCreatedAtDesc(Long barId);
    boolean existsByUserIdAndBarId(Long userId, Long barId);
}
