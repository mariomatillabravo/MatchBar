package com.matchbar.repository;

import com.matchbar.entity.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByBarIdOrderByCreatedAtDesc(String barId);
    List<Review> findByUserIdOrderByCreatedAtDesc(String userId);
    boolean existsByUserIdAndBarId(String userId, String barId);
}
