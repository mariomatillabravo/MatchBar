package com.matchbar.controller;

import com.matchbar.dto.response.BarResponse;
import com.matchbar.security.UserPrincipal;
import com.matchbar.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/favorites")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ResponseEntity<List<BarResponse>> list(@AuthenticationPrincipal UserPrincipal me) {
        return ResponseEntity.ok(favoriteService.listByUser(me.getId()));
    }

    @PostMapping("/{barId}")
    public ResponseEntity<Void> add(@PathVariable String barId,
                                    @AuthenticationPrincipal UserPrincipal me) {
        favoriteService.add(me.getId(), barId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{barId}")
    public ResponseEntity<Void> remove(@PathVariable String barId,
                                       @AuthenticationPrincipal UserPrincipal me) {
        favoriteService.remove(me.getId(), barId);
        return ResponseEntity.noContent().build();
    }
}
