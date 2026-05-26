package com.matchbar.service;

import com.matchbar.dto.request.ReviewRequest;
import com.matchbar.dto.response.ReviewResponse;
import com.matchbar.entity.Bar;
import com.matchbar.entity.Review;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BarRepository barRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse addReview(Long userId, Long barId, ReviewRequest req) {
        if (reviewRepository.existsByUserIdAndBarId(userId, barId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya has valorado este bar");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Bar bar = barRepository.findById(barId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bar no encontrado"));

        Review review = Review.builder()
                .user(user)
                .bar(bar)
                .ratingAtmosphere(req.ratingAtmosphere())
                .ratingFood(req.ratingFood())
                .ratingPrice(req.ratingPrice())
                .comment(req.comment())
                .build();
        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listByBar(Long barId) {
        return reviewRepository.findByBarIdOrderByCreatedAtDesc(barId).stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }
}
