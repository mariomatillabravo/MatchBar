package com.matchbar.controller;

import com.matchbar.dto.request.BarUpsertRequest;
import com.matchbar.dto.request.ReviewRequest;
import com.matchbar.dto.response.BarResponse;
import com.matchbar.dto.response.ReviewResponse;
import com.matchbar.security.UserPrincipal;
import com.matchbar.service.BarService;
import com.matchbar.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bars")
@RequiredArgsConstructor
public class BarController {

    private final BarService barService;
    private final ReviewService reviewService;

    /** Búsqueda pública de bares cercanos. */
    @GetMapping("/nearby")
    public ResponseEntity<List<BarResponse>> nearby(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false) Long matchId,
            @RequestParam(required = false) Integer radiusMeters) {
        return ResponseEntity.ok(barService.findNearby(lat, lng, matchId, radiusMeters));
    }

    /** Ficha pública de un bar. */
    @GetMapping("/{id}")
    public ResponseEntity<BarResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(barService.getById(id));
    }

    /** Reseñas de un bar. */
    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewResponse>> reviews(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.listByBar(id));
    }

    /** Crear reseña: solo USER. */
    @PostMapping("/{id}/reviews")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReviewResponse> addReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest req,
            @AuthenticationPrincipal UserPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.addReview(me.getId(), id, req));
    }

    /** Crear o actualizar la ficha del bar autenticado. */
    @PostMapping("/me")
    @PreAuthorize("hasRole('BAR')")
    public ResponseEntity<BarResponse> upsertMine(
            @Valid @RequestBody BarUpsertRequest req,
            @AuthenticationPrincipal UserPrincipal me) {
        return ResponseEntity.ok(barService.createOrUpdateForUser(me.getId(), req));
    }

    /** Obtener mi propia ficha (rol BAR). */
    @GetMapping("/me")
    @PreAuthorize("hasRole('BAR')")
    public ResponseEntity<BarResponse> getMine(@AuthenticationPrincipal UserPrincipal me) {
        return ResponseEntity.ok(barService.getMine(me.getId()));
    }
}
