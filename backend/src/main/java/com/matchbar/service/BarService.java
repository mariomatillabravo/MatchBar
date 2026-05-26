package com.matchbar.service;

import com.matchbar.dto.request.BarUpsertRequest;
import com.matchbar.dto.response.BarResponse;
import com.matchbar.entity.Bar;
import com.matchbar.entity.User;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.BarRepository;
import com.matchbar.repository.BroadcastRepository;
import com.matchbar.repository.ReviewRepository;
import com.matchbar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BarService {

    private final BarRepository barRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final BroadcastRepository broadcastRepository;
    private final MongoTemplate mongoTemplate;

    public BarResponse createOrUpdateForUser(String userId, BarUpsertRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (user.getRole() != User.Role.BAR) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo usuarios con rol BAR pueden tener ficha de bar");
        }
        Bar bar = barRepository.findByUserId(userId).orElseGet(() ->
                Bar.builder().userId(userId).build());
        bar.setName(req.name());
        bar.setDescription(req.description());
        bar.setAddress(req.address());
        bar.setLatitude(req.latitude());
        bar.setLongitude(req.longitude());
        bar.setLocation(new GeoJsonPoint(req.longitude().doubleValue(), req.latitude().doubleValue()));
        if (req.licenseDoc() != null) bar.setLicenseDoc(req.licenseDoc());
        if (req.photoUrl() != null) bar.setPhotoUrl(req.photoUrl());
        bar = barRepository.save(bar);
        return BarResponse.from(bar, null, computeAverage(bar.getId()));
    }

    public BarResponse getById(String id) {
        Bar bar = barRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bar no encontrado"));
        return BarResponse.from(bar, null, computeAverage(bar.getId()));
    }

    public BarResponse getMine(String userId) {
        Bar bar = barRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Aún no has creado tu ficha de bar"));
        return BarResponse.from(bar, null, computeAverage(bar.getId()));
    }

    public List<BarResponse> findNearby(Double lat, Double lng, String matchId, Integer radiusMeters) {
        int r = radiusMeters != null ? radiusMeters : 5000;

        Criteria criteria = Criteria.where("status").is(Bar.Status.APPROVED);

        if (matchId != null) {
            Set<String> barIds = broadcastRepository.findByMatchId(matchId).stream()
                    .map(b -> b.getBarId())
                    .collect(Collectors.toSet());
            if (barIds.isEmpty()) return List.of();
            criteria = criteria.and("_id").in(barIds);
        }

        NearQuery nearQuery = NearQuery.near(new Point(lng, lat))
                .spherical(true)
                .maxDistance(new Distance(r / 1000.0, Metrics.KILOMETERS))
                .query(new org.springframework.data.mongodb.core.query.Query(criteria));

        return mongoTemplate.geoNear(nearQuery, Bar.class).getContent().stream()
                .map(gr -> BarResponse.from(gr.getContent(),
                        gr.getDistance().getValue() * 1000.0,
                        computeAverage(gr.getContent().getId())))
                .collect(Collectors.toList());
    }

    public java.util.Map<String, Long> getStatusStats() {
        return java.util.Map.of(
                "pending",  barRepository.countByStatus(Bar.Status.PENDING),
                "approved", barRepository.countByStatus(Bar.Status.APPROVED),
                "rejected", barRepository.countByStatus(Bar.Status.REJECTED)
        );
    }

    public List<BarResponse> listPending() {
        return barRepository.findByStatus(Bar.Status.PENDING).stream()
                .map(b -> BarResponse.from(b, null, computeAverage(b.getId())))
                .collect(Collectors.toList());
    }

    public BarResponse setStatus(String barId, Bar.Status status) {
        Bar bar = barRepository.findById(barId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bar no encontrado"));
        bar.setStatus(status);
        return BarResponse.from(barRepository.save(bar), null, computeAverage(bar.getId()));
    }

    private Double computeAverage(String barId) {
        var reviews = reviewRepository.findByBarIdOrderByCreatedAtDesc(barId);
        if (reviews.isEmpty()) return null;
        double sum = reviews.stream()
                .mapToDouble(r -> (r.getRatingAtmosphere() + r.getRatingFood() + r.getRatingPrice()) / 3.0)
                .sum();
        return Math.round((sum / reviews.size()) * 10) / 10.0;
    }
}
