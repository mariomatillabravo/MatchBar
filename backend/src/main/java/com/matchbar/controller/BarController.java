package com.matchbar.controller;

import com.matchbar.dto.request.BarUpsertRequest;
import com.matchbar.dto.request.ReviewRequest;
import com.matchbar.dto.response.BarResponse;
import com.matchbar.dto.response.MatchResponse;
import com.matchbar.dto.response.ReviewResponse;
import com.matchbar.security.UserPrincipal;
import com.matchbar.service.BarService;
import com.matchbar.service.LicenseDocService;
import com.matchbar.service.MatchService;
import com.matchbar.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bars")
@RequiredArgsConstructor
public class BarController {

    private final BarService barService;
    private final ReviewService reviewService;
    private final LicenseDocService licenseDocService;
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
}
