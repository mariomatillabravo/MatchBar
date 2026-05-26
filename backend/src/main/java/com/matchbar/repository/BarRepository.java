package com.matchbar.repository;

import com.matchbar.entity.Bar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BarRepository extends JpaRepository<Bar, Long> {

    Optional<Bar> findByUserId(Long userId);

    List<Bar> findByStatus(Bar.Status status);

    /**
     * Busca bares aprobados dentro de un radio en metros.
     * Usa la fórmula de Haversine en SQL.
     */
    @Query(value = """
        SELECT * FROM bars b
        WHERE b.status = 'APPROVED'
          AND (6371000 * acos(
                cos(radians(:lat)) * cos(radians(b.latitude)) *
                cos(radians(b.longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(b.latitude))
              )) <= :radiusMeters
        ORDER BY (6371000 * acos(
                cos(radians(:lat)) * cos(radians(b.latitude)) *
                cos(radians(b.longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(b.latitude))
              )) ASC
        """, nativeQuery = true)
    List<Bar> findApprovedWithinRadius(@Param("lat") Double lat,
                                       @Param("lng") Double lng,
                                       @Param("radiusMeters") Integer radiusMeters);

    /**
     * Busca bares aprobados que retransmiten un partido específico,
     * dentro de un radio en metros.
     */
    @Query(value = """
        SELECT b.* FROM bars b
        JOIN broadcasts br ON br.bar_id = b.id
        WHERE b.status = 'APPROVED'
          AND br.match_id = :matchId
          AND (6371000 * acos(
                cos(radians(:lat)) * cos(radians(b.latitude)) *
                cos(radians(b.longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(b.latitude))
              )) <= :radiusMeters
        ORDER BY (6371000 * acos(
                cos(radians(:lat)) * cos(radians(b.latitude)) *
                cos(radians(b.longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(b.latitude))
              )) ASC
        """, nativeQuery = true)
    List<Bar> findApprovedByMatchWithinRadius(@Param("matchId") Long matchId,
                                              @Param("lat") Double lat,
                                              @Param("lng") Double lng,
                                              @Param("radiusMeters") Integer radiusMeters);
}
