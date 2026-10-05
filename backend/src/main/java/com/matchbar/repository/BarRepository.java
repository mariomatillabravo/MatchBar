package com.matchbar.repository;

import com.matchbar.entity.Bar;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface BarRepository extends MongoRepository<Bar, String> {
    Optional<Bar> findByUserId(String userId);
    List<Bar> findByStatus(Bar.Status status);
    long countByStatus(Bar.Status status);

    /** True si la imagen es una foto o una página de la carta de algún bar. */
    @Query(value = "{ '$or': [ { 'photoFileIds': ?0 }, { 'menuFileIds': ?0 } ] }", exists = true)
    boolean existsByImageFileId(String fileId);
}
