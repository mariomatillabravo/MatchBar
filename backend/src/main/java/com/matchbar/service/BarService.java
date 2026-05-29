package com.matchbar.service;

import com.matchbar.dto.request.BarAdminUpdateRequest;
import com.matchbar.dto.request.BarUpsertRequest;
import com.matchbar.dto.response.BarAdminResponse;
import com.matchbar.dto.response.BarResponse;
import com.matchbar.dto.response.PendingBarResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
    private final GeocodingService geocodingService;

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

        // El dueño solo escribe la dirección; aquí la geocodificamos a lat/lng.
        // Solo llamamos a Nominatim si la dirección es nueva o ha cambiado, para
        // evitar peticiones innecesarias al guardar otros campos.
        boolean needsGeocoding = bar.getLocation() == null
                || !req.address().trim().equalsIgnoreCase(
                        bar.getAddress() == null ? "" : bar.getAddress().trim());
        bar.setAddress(req.address());
        if (needsGeocoding) {
            applyGeocodedLocation(bar, req.address());
        }

        if (req.ownerPhone() != null) bar.setOwnerPhone(req.ownerPhone());
        if (req.photoUrl() != null) bar.setPhotoUrl(req.photoUrl());
        bar = barRepository.save(bar);
        return BarResponse.from(bar, null, computeAverage(bar.getId()));
    }

    /** Resuelve la dirección a coordenadas y las vuelca en la entidad (lat, lng y punto geoespacial). */
    private void applyGeocodedLocation(Bar bar, String address) {
        GeocodingService.Coordinates coords = geocodingService.geocode(address);
        bar.setLatitude(BigDecimal.valueOf(coords.latitude()));
        bar.setLongitude(BigDecimal.valueOf(coords.longitude()));
        bar.setLocation(new GeoJsonPoint(coords.longitude(), coords.latitude()));
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

    public List<BarResponse> findAllApproved() {
        return barRepository.findByStatus(Bar.Status.APPROVED).stream()
                .map(b -> BarResponse.from(b, null, computeAverage(b.getId())))
                .collect(Collectors.toList());
    }

    public java.util.Map<String, Long> getStatusStats() {
        return java.util.Map.of(
                "pending",  barRepository.countByStatus(Bar.Status.PENDING),
                "approved", barRepository.countByStatus(Bar.Status.APPROVED),
                "rejected", barRepository.countByStatus(Bar.Status.REJECTED)
        );
    }

    public List<PendingBarResponse> listPending() {
        List<Bar> bars = barRepository.findByStatus(Bar.Status.PENDING);
        Set<String> ownerIds = bars.stream().map(Bar::getUserId).collect(Collectors.toSet());
        Map<String, User> ownersById = userRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return bars.stream().map(b -> {
            User owner = ownersById.get(b.getUserId());
            String licenseUrl = b.getLicenseDocFileId() != null
                    ? "/api/admin/bars/" + b.getId() + "/license"
                    : null;
            return new PendingBarResponse(
                    b.getId(), b.getName(), b.getAddress(),
                    owner != null ? owner.getName() : null,
                    owner != null ? owner.getEmail() : null,
                    b.getOwnerPhone(),
                    b.getLicenseDocFilename(),
                    licenseUrl
            );
        }).collect(Collectors.toList());
    }

    public String replaceLicense(String userId, MultipartFile file, LicenseDocService licenseDocService) {
        Bar bar = barRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Aún no has creado tu ficha de bar"));
        if (bar.getLicenseDocFileId() != null) {
            licenseDocService.delete(bar.getLicenseDocFileId());
        }
        String fileId = licenseDocService.store(file);
        bar.setLicenseDocFileId(fileId);
        bar.setLicenseDocFilename(file.getOriginalFilename());
        barRepository.save(bar);
        return file.getOriginalFilename();
    }

    public List<BarAdminResponse> listAll() {
        List<Bar> bars = barRepository.findByStatus(Bar.Status.APPROVED);
        bars.sort(java.util.Comparator.comparing(Bar::getCreatedAt,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        Set<String> ownerIds = bars.stream().map(Bar::getUserId).collect(Collectors.toSet());
        Map<String, User> ownersById = userRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return bars.stream().map(b -> toAdminResponse(b, ownersById.get(b.getUserId()))).collect(Collectors.toList());
    }

    public BarAdminResponse getAdminDetail(String barId) {
        Bar bar = barRepository.findById(barId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bar no encontrado"));
        User owner = bar.getUserId() != null ? userRepository.findById(bar.getUserId()).orElse(null) : null;
        return toAdminResponse(bar, owner);
    }

    public BarAdminResponse adminUpdate(String barId, BarAdminUpdateRequest req) {
        Bar bar = barRepository.findById(barId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bar no encontrado"));
        bar.setName(req.name());
        bar.setDescription(req.description());
        bar.setAddress(req.address());
        bar.setLatitude(req.latitude());
        bar.setLongitude(req.longitude());
        bar.setLocation(new GeoJsonPoint(req.longitude().doubleValue(), req.latitude().doubleValue()));
        bar.setOwnerPhone(req.ownerPhone());
        bar.setPhotoUrl(req.photoUrl());
        bar = barRepository.save(bar);
        User owner = bar.getUserId() != null ? userRepository.findById(bar.getUserId()).orElse(null) : null;
        return toAdminResponse(bar, owner);
    }

    public void adminDelete(String barId, LicenseDocService licenseDocService) {
        Bar bar = barRepository.findById(barId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bar no encontrado"));
        if (bar.getLicenseDocFileId() != null) {
            licenseDocService.delete(bar.getLicenseDocFileId());
        }
        barRepository.delete(bar);
    }

    private BarAdminResponse toAdminResponse(Bar b, User owner) {
        return new BarAdminResponse(
                b.getId(), b.getUserId(), b.getName(), b.getDescription(), b.getAddress(),
                b.getLatitude(), b.getLongitude(), b.getOwnerPhone(), b.getPhotoUrl(),
                b.getLicenseDocFilename(), b.getStatus(),
                owner != null ? owner.getName() : null,
                owner != null ? owner.getEmail() : null,
                computeAverage(b.getId()),
                b.getCreatedAt()
        );
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
