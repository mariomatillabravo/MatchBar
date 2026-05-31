package com.matchbar.controller;

import com.matchbar.dto.response.MyReviewResponse;
import com.matchbar.security.UserPrincipal;
import com.matchbar.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class UserController {

    private final ReviewService reviewService;

    @GetMapping("/reviews")
    public ResponseEntity<List<MyReviewResponse>> myReviews(@AuthenticationPrincipal UserPrincipal me) {
        return ResponseEntity.ok(reviewService.listMine(me.getId()));
    }
}
