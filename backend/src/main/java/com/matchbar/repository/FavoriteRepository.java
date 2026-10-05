package com.matchbar.repository;

import com.matchbar.entity.Favorite;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface FavoriteRepository extends MongoRepository<Favorite, String> {
    List<Favorite> findByUserId(String userId);
    boolean existsByUserIdAndBarId(String userId, String barId);
    void deleteByBarId(String barId);
    void deleteByUserId(String userId);
    void deleteByUserIdAndBarId(String userId, String barId);
}
