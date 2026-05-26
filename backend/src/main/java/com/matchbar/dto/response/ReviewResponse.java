package com.matchbar.dto.response;

import com.matchbar.entity.Review;
import java.time.Instant;

public record ReviewResponse(
        String id,
        String userId,
        String userName,
        Integer ratingAtmosphere,
        Integer ratingFood,
        Integer ratingPrice,
        Double average,
        String comment,
        Instant createdAt
) {
    public static ReviewResponse from(Review r) {
        double avg = (r.getRatingAtmosphere() + r.getRatingFood() + r.getRatingPrice()) / 3.0;
        return new ReviewResponse(
                r.getId(),
                r.getUserId(),
                r.getUserName(),
                r.getRatingAtmosphere(),
                r.getRatingFood(),
                r.getRatingPrice(),
                Math.round(avg * 10) / 10.0,
                r.getComment(),
                r.getCreatedAt()
        );
    }
}
