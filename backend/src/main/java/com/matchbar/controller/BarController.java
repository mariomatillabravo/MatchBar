package com.matchbar.controller;

import com.matchbar.dto.request.BarUpsertRequest;
import com.matchbar.dto.request.ReviewRequest;
import com.matchbar.dto.response.BarResponse;
import com.matchbar.dto.response.MatchResponse;
import com.matchbar.dto.response.ReviewResponse;
import com.matchbar.security.UserPrincipal;
import com.matchbar.service.BarService;
import com.matchbar.service.ImageService;
import com.matchbar.service.LicenseDocService;
import com.matchbar.service.MatchService;
import com.matchbar.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bars")
@RequiredArgsConstructor
public class BarController {

    private final BarService barService;
    private final ReviewService reviewService;
    private final LicenseDocService licenseDocService;
    private final ImageService imageService;
    private final MatchService matchService;

    @GetMapping
    public ResponseEntity<List<BarResponse>> all() {
        return ResponseEntity.ok(barService.findAllApproved());
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<BarResponse>> nearby(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false) String matchId,
            @RequestParam(required = false) Integer radiusMeters) {
        return ResponseEntity.ok(barService.findNearby(lat, lng, matchId, radiusMeters));
    }

    @GetMapping("/{id}/matches")
    public ResponseEntity<List<MatchResponse>> upcomingMatches(@PathVariable String id) {
        return ResponseEntity.ok(matchService.upcomingByBar(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BarResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(barService.getById(id));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewResponse>> reviews(@PathVariable String id) {
        return ResponseEntity.ok(reviewService.listByBar(id));
    }

    @PostMapping("/{id}/reviews")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReviewResponse> addReview(
            @PathVariable String id,
            @Valid @RequestBody ReviewRequest req,
            @AuthenticationPrincipal UserPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.addReview(me.getId(), id, req));
    }

    @PostMapping("/me")
    @PreAuthorize("hasRole('BAR')")
    public ResponseEntity<BarResponse> upsertMine(
            @Valid @RequestBody BarUpsertRequest req,
            @AuthenticationPrincipal UserPrincipal me) {
        return ResponseEntity.ok(barService.createOrUpdateForUser(me.getId(), req));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('BAR')")
    public ResponseEntity<BarResponse> getMine(@AuthenticationPrincipal UserPrincipal me) {
        return ResponseEntity.ok(barService.getMine(me.getId()));
    }

    @PostMapping(value = "/me/license", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('BAR')")
    public ResponseEntity<Map<String, String>> uploadLicense(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal me) {
        String filename = barService.replaceLicense(me.getId(), file, licenseDocService);
        return ResponseEntity.ok(Map.of("filename", filename));
    }

    // ----- Imágenes del bar (fotos del establecimiento y carta) -----

    @PostMapping(value = "/me/photos", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('BAR')")
    public ResponseEntity<BarResponse> addPhoto(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal me) {
        return ResponseEntity.ok(barService.addPhoto(me.getId(), file, imageService));
    }

    @DeleteMapping("/me/photos/{fileId}")
    @PreAuthorize("hasRole('BAR')")
    public ResponseEntity<BarResponse> removePhoto(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal me) {
        return ResponseEntity.ok(barService.removePhoto(me.getId(), fileId, imageService));
    }

    @PostMapping(value = "/me/menu", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('BAR')")
    public ResponseEntity<BarResponse> uploadMenu(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal me) {
        return ResponseEntity.ok(barService.setMenu(me.getId(), file, imageService));
    }

    /** Sirve una imagen del bar. Público para que los clientes (Coil) puedan cargarla sin token. */
    @GetMapping("/images/{fileId}")
    public ResponseEntity<InputStreamResource> image(@PathVariable String fileId) throws IOException {
        GridFsResource resource = imageService.load(fileId);
        String contentType = resource.getContentType();
        MediaType mediaType = contentType != null
                ? MediaType.parseMediaType(contentType) : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new InputStreamResource(resource.getInputStream()));
    }
}
