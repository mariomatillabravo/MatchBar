package com.matchbar.service;

import com.matchbar.dto.request.ReviewRequest;
import com.matchbar.dto.response.MyReviewResponse;
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BarRepository barRepository;
    private final UserRepository userRepository;

    public ReviewResponse addReview(String userId, String barId, ReviewRequest req) {
        if (reviewRepository.existsByUserIdAndBarId(userId, barId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya has valorado este bar");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (!barRepository.existsById(barId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Bar no encontrado");
        }
        Review review = Review.builder()
                .userId(userId)
                .userName(user.getName())
                .barId(barId)
                .ratingAtmosphere(req.ratingAtmosphere())
                .ratingFood(req.ratingFood())
                .ratingPrice(req.ratingPrice())
                .comment(req.comment())
                .build();
        return ReviewResponse.from(reviewRepository.save(review));
    }

    public List<ReviewResponse> listByBar(String barId) {
        return reviewRepository.findByBarIdOrderByCreatedAtDesc(barId).stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    public List<MyReviewResponse> listMine(String userId) {
        List<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Set<String> barIds = reviews.stream().map(Review::getBarId).collect(Collectors.toSet());
        Map<String, String> barNamesById = barRepository.findAllById(barIds).stream()
                .collect(Collectors.toMap(Bar::getId, Bar::getName));
        return reviews.stream().map(r -> {
            double avg = (r.getRatingAtmosphere() + r.getRatingFood() + r.getRatingPrice()) / 3.0;
            return new MyReviewResponse(
                    r.getId(),
                    r.getBarId(),
                    barNamesById.getOrDefault(r.getBarId(), "Bar eliminado"),
                    r.getRatingAtmosphere(),
                    r.getRatingFood(),
                    r.getRatingPrice(),
                    Math.round(avg * 10) / 10.0,
                    r.getComment(),
                    r.getCreatedAt()
            );
        }).collect(Collectors.toList());
    }
}
