package com.matchbar.service;

import com.matchbar.dto.request.BarUpsertRequest;
import com.matchbar.dto.response.BarResponse;
import com.matchbar.entity.Bar;
import com.matchbar.entity.User;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.BarRepository;
import com.matchbar.repository.ReviewRepository;
import com.matchbar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BarService {

    private final BarRepository barRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public BarResponse createOrUpdateForUser(Long userId, BarUpsertRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (user.getRole() != User.Role.BAR) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo usuarios con rol BAR pueden tener ficha de bar");
        }
        Bar bar = barRepository.findByUserId(userId).orElseGet(() ->
                Bar.builder().user(user).status(Bar.Status.PENDING).build());
        bar.setName(req.name());
        bar.setDescription(req.description());
        bar.setAddress(req.address());
        bar.setLatitude(req.latitude());
        bar.setLongitude(req.longitude());
        if (req.licenseDoc() != null) bar.setLicenseDoc(req.licenseDoc());
        if (req.photoUrl() != null) bar.setPhotoUrl(req.photoUrl());
        bar = barRepository.save(bar);
        return BarResponse.from(bar, null, computeAverage(bar.getId()));
    }

    @Transactional(readOnly = true)
    public BarResponse getById(Long id) {
        Bar bar = barRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bar no encontrado"));
        return BarResponse.from(bar, null, computeAverage(bar.getId()));
    }

    @Transactional(readOnly = true)
    public BarResponse getMine(Long userId) {
        Bar bar = barRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Aún no has creado tu ficha de bar"));
        return BarResponse.from(bar, null, computeAverage(bar.getId()));
    }

    @Transactional(readOnly = true)
    public List<BarResponse> findNearby(Double lat, Double lng, Long matchId, Integer radiusMeters) {
        int r = radiusMeters != null ? radiusMeters : 5000;
        List<Bar> bars = (matchId == null)
                ? barRepository.findApprovedWithinRadius(lat, lng, r)
                : barRepository.findApprovedByMatchWithinRadius(matchId, lat, lng, r);
        return bars.stream()
                .map(b -> BarResponse.from(b,
                        haversine(lat, lng, b.getLatitude().doubleValue(), b.getLongitude().doubleValue()),
                        computeAverage(b.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BarResponse> listPending() {
        return barRepository.findByStatus(Bar.Status.PENDING).stream()
                .map(b -> BarResponse.from(b, null, computeAverage(b.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public BarResponse setStatus(Long barId, Bar.Status status) {
        Bar bar = barRepository.findById(barId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bar no encontrado"));
        bar.setStatus(status);
        return BarResponse.from(barRepository.save(bar), null, computeAverage(bar.getId()));
    }

    private Double computeAverage(Long barId) {
        var reviews = reviewRepository.findByBarIdOrderByCreatedAtDesc(barId);
        if (reviews.isEmpty()) return null;
        double sum = reviews.stream()
                .mapToDouble(r -> (r.getRatingAtmosphere() + r.getRatingFood() + r.getRatingPrice()) / 3.0)
                .sum();
        return Math.round((sum / reviews.size()) * 10) / 10.0;
    }

    /** Distancia Haversine en metros (radio de la Tierra: 6_371_000 m). */
    public static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double R = 6_371_000d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
